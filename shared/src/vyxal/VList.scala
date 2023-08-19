package vyxal

import vyxal.VNum.given

import scala.collection.immutable.SeqOps
import scala.collection.mutable
import scala.collection.SpecificIterableFactory

/** A Vyxal list. It simply wraps around another list and could represent a
  * completely evaluated list, a finite lazy list that is in the process of
  * being evaluated, or an infinite list.
  *
  * To construct a VList, use VList.apply or VList.from
  * @param lst
  *   The wrapped list actually holdings this VList's elements.
  */
class VList private (val lst: Seq[VAny])
    extends Seq[VAny],
      SeqOps[VAny, Seq, VList]:

  /** Map the list using a Vyxal function */
  def vmap(f: VAny => Context ?=> VAny)(using Context): VList =
    new VList(lst.map(f(_)))

  /** Zip two VLists together with a function. If one is longer than the other,
    * keep the longer one's elements as-is.
    */
  def zipWith(other: VList)(f: (VAny, VAny) => Context ?=> VAny)(using
      ctx: Context
  ): VList =
    new VList(
      lst
        .zipAll(other.lst, ctx.settings.defaultValue, ctx.settings.defaultValue)
        .map(f(_, _))
    )

  /** Zip two VLists together without a function. If one is longer than the
    * other, keep the longer one's elements as-is.
    */

  def zip(other: VList)(using ctx: Context): VList =
    val temp = lst
      .zipAll(
        other.lst,
        ctx.settings.defaultValue,
        ctx.settings.defaultValue
      )
      .map(VList(_, _))
    VList(temp*)

  /** Get the element at index `ind` */
  override def apply(ind: Int): VAny =
    if lst.isEmpty then 0
    else if ind < 0 then
      // floorMod because % gives negative results with negative dividends
      lst(Math.floorMod(ind, lst.length))
    else
      try lst(ind)
      catch case _: IndexOutOfBoundsException => lst(ind % lst.length)

  /** Override to specify return type as VList */
  override def take(n: Int): VList = VList.from(lst.take(n))

  /** Override to specify return type as VList */
  override def drop(n: Int): VList = VList.from(lst.drop(n))

  /** Override to specify return type as VList */
  override def dropRight(n: Int): VList = VList.from(lst.dropRight(n))

  def index(ind: VAny)(using ctx: Context): VAny =
    ind match
      case ind: VNum => this.indexBig(ind.real.toBigInt)
      case inds: VList => inds.vmap(this.index)
      case _ => throw new Exception("Index must be a number")

  private def indexBig(ind: BigInt): VAny =
    if ind <= Int.MaxValue && ind >= Int.MinValue then return apply(ind.toInt)
    var pos = if ind < 0 then ind % lst.length else ind
    var temp = lst
    while pos > 0 do
      // Instead of using modulo, reset the list if out of bounds
      if temp.isEmpty then temp = lst
      temp = temp.tail
      pos -= 1
    temp.head

  override def iterator: Iterator[VAny] = lst.iterator

  /** Get the length of this `VList`. A word of caution: this fully evaluates
    * the list, meaning that it won't work with infinite lists.
    */
  override def length: Int = lst.length

  /** This violates the method contract, since [[List]]s actually need a
    * traversal to get their length, but it helps us check for lazy lists
    */
  override def knownSize: Int = lst match
    case _: List[?] => lst.size
    case _ => lst.knownSize

  /** Overridden to preserve laziness */
  override def map[B](f: VAny => B): Seq[B] = lst.map(f)

  /** This isn't an overload of isDefinedAt because it needs to take a `BigInt`
    */
  def hasIndex(ind: BigInt): Boolean =
    if ind <= Int.MaxValue && ind >= 0 then return lst.isDefinedAt(ind.toInt)
    var pos = if ind < 0 then ind % lst.length else ind
    var temp = lst
    while pos >= Int.MaxValue do
      // Instead of using modulo, reset the list if out of bounds
      if temp.isEmpty then return false
      temp = temp.drop(Int.MaxValue)
      pos -= Int.MaxValue
    return true

  override def toString(): String =
    lst.map(_.toString).mkString("[ ", " | ", " ]")

  override protected def fromSpecific(coll: IterableOnce[VAny]): VList =
    VList.fromSpecific(coll)

  override protected def newSpecificBuilder
      : collection.mutable.Builder[VAny, VList] =
    VList.newBuilder

  override def empty: VList = VList.empty

  protected def from(it: Seq[VAny]): VList =
    VList.from(it)

  override def equals(o: Any): Boolean = o match
    case v: VList => this.lst == v.lst
    case _ => this.lst == o

  override def hashCode(): Int = this.lst.hashCode()

  def distinct(using Context): VList =
    val uniq: LazyList[Option[VAny]] =
      LazyList.unfold(Seq[VAny]() -> 0) { state =>
        if !this.hasIndex(state._2) then None
        else if state._1.contains(this.index(state._2)) then
          Some(None, state._1 -> (state._2 + 1))
        else
          Some(
            Some(this.index(state._2)),
            (state._1 :+ this.index(state._2)) -> (state._2 + 1)
          )
      }

    VList.from(uniq.flatten)
end VList

object VList extends SpecificIterableFactory[VAny, VList]:
  def from(it: Seq[VAny]): VList =
    it match
      case temp: VList => temp
      case _ => new VList(it)

  /** Zip multiple VLists together with a function.
    *
    * The parameter is a `PartialFunction` instead of a function because it's
    * going to match on a list and assume it's a specific length
    */
  def zipMulti(lists: VList*)(f: PartialFunction[Seq[VAny], VAny])(using
      ctx: Context
  ): VList =
    val maxSize = lists.view.map(_.size).max
    val padded = lists.map { list =>
      if list.sizeIs == maxSize then list
      else list ++ Seq.fill(maxSize - list.size)(null)
    }
    new VList(padded.transpose.map { lst => f(lst.filter(_ != null)) })

  /** Turn some VAnys into iterables, then zip them together with a function. */
  def zipValues(values: VAny*)(f: PartialFunction[Seq[VAny], VAny])(using
      ctx: Context
  ): VList =
    val filteredLists = values.collect { case l: VList => l }
    val lists =
      if values.size == filteredLists.size then filteredLists
      else if filteredLists.isEmpty then values.map(ListHelpers.makeIterable(_))
      else
        val maxSize = filteredLists.view.map(_.size).max
        values.map {
          case l: VList => l
          case x =>
            // If one of the other elements is a list but this isn't, repeat
            // this one to be as long as that list
            VList.fill(maxSize)(x)
        }
    VList.zipMulti(lists*)(f)

  /** This lets us pattern match on `VList`s, silly as the implementation may
    * be.
    */
  def unapplySeq(vlist: VList): Seq[VAny] = vlist.lst

  override def empty: VList = new VList(Seq.empty)

  override def newBuilder: mutable.Builder[VAny, VList] =
    mutable.ArrayBuffer
      .newBuilder[VAny]
      .mapResult(elems => new VList(elems.toSeq))

  override def fromSpecific(it: IterableOnce[VAny]): VList = new VList(
    it.iterator.toSeq
  )

  def seqToVList(seq: Seq[Seq[VAny]]): VList = new VList(seq.map(VList.from))
end VList

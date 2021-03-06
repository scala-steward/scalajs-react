package japgolly.scalajs.react

import japgolly.scalajs.react.util.Effect.Sync

trait ReactExtensions {
  import ReactExtensions._

  @inline final implicit def ReactExt_OptionSync[F[_]](o: Option[F[Unit]]): ReactExt_OptionSyncUnit[F] =
    new ReactExt_OptionSyncUnit(o)

  @inline final implicit def ReactExt_ScalaComponent[P, S, B, CT[-p, +u] <: CtorType[p, u]](c: ScalaComponent.Component[P, S, B, CT]): ReactExt_ScalaComponent[P, S, B, CT] =
    new ReactExt_ScalaComponent(c)

  @inline final implicit def ReactExtrasExt_Any[A](a: A): ReactExtrasExt_Any[A] =
    new ReactExtrasExt_Any(a)
}

object ReactExtensions {

  @inline implicit final class ReactExt_OptionSyncUnit[F[_]](private val o: Option[F[Unit]]) extends AnyVal {
    /** Convenience for `.getOrElse(Callback.empty)` */
    @inline def getOrEmpty(implicit F: Sync[F]): F[Unit] =
       o.getOrElse(F.empty)
  }

  // I am NOT happy about this here... but it will do for now.

  implicit final class ReactExt_ScalaComponent[P, S, B, CT[-p, +u] <: CtorType[p, u]](private val self: ScalaComponent.Component[P, S, B, CT]) extends AnyVal {
    def withRef(ref: Ref.Handle[ScalaComponent.RawMounted[P, S, B]]): ScalaComponent.Component[P, S, B, CT] =
      self.mapCtorType(ct =>
        CtorType.hackBackToSelf[CT, P, ScalaComponent.Unmounted[P, S, B]](ct)(
          ct.withRawProp("ref", ref.raw)
        )
      )(self.ctorPF)

    @deprecated("Use .withOptionalRef", "1.7.0")
    def withRef(r: Option[Ref.Handle[ScalaComponent.RawMounted[P, S, B]]]): ScalaComponent.Component[P, S, B, CT] =
      withOptionalRef(r)

    def withOptionalRef(optionalRef: Option[Ref.Handle[ScalaComponent.RawMounted[P, S, B]]]): ScalaComponent.Component[P, S, B, CT] =
      optionalRef match {
        case None    => self
        case Some(r) => withRef(r)
      }
  }

  implicit final class ReactExtrasExt_Any[A](private val self: A) extends AnyVal {
    @inline def ~=~(a: A)(implicit r: Reusability[A]): Boolean = r.test(self, a)
    @inline def ~/~(a: A)(implicit r: Reusability[A]): Boolean = !r.test(self, a)
  }

}
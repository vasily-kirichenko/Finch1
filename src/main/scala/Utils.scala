object Utils {

  object Implicits {

    import com.twitter.util.{Future => TFuture, Promise => TPromise, Return, Throw}
    import scala.concurrent.{Future => SFuture, Promise => SPromise, ExecutionContext}
    import scala.util.{Success, Failure}

    implicit class RichTFuture[A](val f: TFuture[A]) extends AnyVal {
      def asScala(implicit e: ExecutionContext): SFuture[A] = {
        val p: SPromise[A] = SPromise()
        f.respond {
          case Return(value) => val _ = p.success(value)
          case Throw(exception) => val _ = p.failure(exception)
        }

        p.future
      }
    }

    implicit class RichSFuture[A](val f: SFuture[A]) extends AnyVal {
      def asTwitter(implicit e: ExecutionContext): TFuture[A] = {
        val p: TPromise[A] = new TPromise[A]
        f.onComplete {
          case Success(value) => p.setValue(value)
          case Failure(exception) => p.setException(exception)
        }
        p
      }
    }
  }
}
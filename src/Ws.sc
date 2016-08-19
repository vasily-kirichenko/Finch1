import scala.concurrent.ExecutionContext.Implicits.global

Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
val connStr = "jdbc:sqlserver://localhost:1433;databaseName=WL_TEST;integratedSecurity=true;"
//val _ = ConnectionPool.singleton(connStr, "sa", "")
//implicit val session = AutoSession

val sql = "select * from dbo.FormatType where IDFormatType = 10"


//sql"""
//select * from dbo.FormatType where IDFormatType = 10
//""".execute.apply()

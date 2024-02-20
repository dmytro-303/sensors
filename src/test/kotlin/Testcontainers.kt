import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private const val POSTGRES_IMAGE = "postgres:13.3"

object PostgresContainer {
    val instance by lazy {
        startPostgresContainer().also { postgres ->
            val port = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
            val dbName = postgres.databaseName
            System.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:$port/$dbName")
            System.setProperty("spring.datasource.username", postgres.username)
            System.setProperty("spring.datasource.password", postgres.password)

            System.setProperty("spring.flyway.url", "jdbc:postgresql://localhost:$port/$dbName")
            System.setProperty("spring.flyway.user", postgres.username)
            System.setProperty("spring.flyway.password", postgres.password)
        }
    }

    private fun startPostgresContainer() = PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE)).apply {
        withDatabaseName("testDb")
        start()
    }
}


package com.atraparalagato.impl.repository;
import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/**
 * Implementación esqueleto de DataRepository usando base de datos H2.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Conexión a base de datos H2
 * - Operaciones CRUD con SQL
 * - Manejo de transacciones
 * - Mapeo objeto-relacional
 * - Consultas personalizadas
 * - Manejo de errores de BD
 */
public class H2GameRepository extends DataRepository<HexGameState, String> {
    // TODO: Los estudiantes deben definir la configuración de la base de datos
    private static final String DB_URL = "jdbc:h2:./data/atraparalagato";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Connection connection;
    public H2GameRepository() {
        // TODO: Inicializar conexión a H2 y crear tablas si no existen
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            createSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar a la base de datos H2", e);
        }
    }

    @Override
    public HexGameState save(HexGameState entity) {
        // TODO: Implementar guardado en base de datos H2
        String sqlInsert = "MERGE INTO games (id, state) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sqlInsert)) {
            stmt.setString(1, entity.getGameId());
            stmt.setString(2, serializeGameState(entity));
            stmt.executeUpdate();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el juego", e);
        }
    }

    @Override
    public Optional<HexGameState> findById(String id) {
        // TODO: Buscar juego por ID en la base de datos
        String sql = "SELECT state FROM games WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String stateJson = rs.getString("state");
                return Optional.of(deserializeGameState(stateJson, id));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar el juego", e);
        }
    }

    @Override
    public List<HexGameState> findAll() {
        // TODO: Obtener todos los juegos de la base de datos
        String sql = "SELECT id, state FROM games";
        List<HexGameState> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String stateJson = rs.getString("state");
                result.add(deserializeGameState(stateJson, id));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener todos los juegos", e);
        }
        return result;
    }

    @Override
    public List<HexGameState> findWhere(Predicate<HexGameState> condition) {
        // TODO: Implementar búsqueda con condiciones
        // carga todos y filtra en la memoria
        return findAll().stream().filter(condition).collect(Collectors.toList());
    }

    @Override
    public <R> List<R> findAndTransform(Predicate<HexGameState> condition, Function<HexGameState, R> transformer) {
        // TODO: Buscar y transformar en una operación
        return findAll().stream().filter(condition).map(transformer).collect(Collectors.toList());
    }

    @Override
    public long countWhere(Predicate<HexGameState> condition) {
        // TODO: Contar registros que cumplen condición
        return findWhere(condition).size();
    }

    @Override
    public boolean deleteById(String id) {
        // TODO: Eliminar juego por ID
        String sql = "DELETE FROM games WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el juego", e);
        }
    }

    @Override
    public long deleteWhere(Predicate<HexGameState> condition) {
        // TODO: Eliminar múltiples registros según condición
        List<HexGameState> toDelete = findWhere(condition);
        long count = 0;
        for (HexGameState state : toDelete) {
            if (deleteById(state.getGameId())) count++;
        }
        return count;
    }

    @Override
    public boolean existsById(String id) {
        // TODO: Verificar si existe un juego con el ID dado
        String sql = "SELECT COUNT(*) FROM games WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error al verificar existencia", e);
        }
    }

    @Override
    public <R> R executeInTransaction(Function<DataRepository<HexGameState, String>, R> operation) {
        // TODO: Ejecutar operación en transacción
        try {
            connection.setAutoCommit(false);
            R result = operation.apply(this);
            connection.commit();
            connection.setAutoCommit(true);
            return result;
        } catch (Exception e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error en transacción", e);
        }
    }

    @Override
    public List<HexGameState> findWithPagination(int page, int size) {
        // TODO: Implementar paginación con LIMIT y OFFSET
        String sql = "SELECT id, state FROM games LIMIT ? OFFSET ?";
        List<HexGameState> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, size);
            stmt.setInt(2, page * size);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String stateJson = rs.getString("state");
                result.add(deserializeGameState(stateJson, id));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error en paginación", e);
        }
        return result;
    }

    @Override
    public List<HexGameState> findAllSorted(Function<HexGameState, ? extends Comparable<?>> sortKeyExtractor, boolean ascending) {
        // TODO: Implementar ordenamiento
        @SuppressWarnings("rawtypes")
        Comparator<HexGameState> comparator = Comparator.comparing(
            h -> (Comparable) sortKeyExtractor.apply(h),
            Comparator.nullsLast(Comparator.naturalOrder())
        );
        if (!ascending) comparator = comparator.reversed();
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
        // TODO: Ejecutar consulta SQL personalizada
        List<R> results = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                results.add(resultMapper.apply(rs.getObject(1)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error en consulta personalizada", e);
        }
        return results;
    }

    @Override
    protected void initialize() {
        // TODO: Inicializar base de datos
        createSchema();
    }

    @Override
    protected void cleanup() {
        // TODO: Limpiar recursos
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cerrar la conexión", e);
        }
    }

    // Métodos auxiliares que los estudiantes pueden implementar
    /**
     * TODO: Crear el esquema de la base de datos.
     * Definir tablas, columnas, tipos de datos, restricciones.
     */
    private void createSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS games (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "state CLOB NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el esquema", e);
        }
    }

    /**
     * TODO: Serializar HexGameState a formato de BD.
     * Puede usar JSON, XML, o campos separados.
     */
    private String serializeGameState(HexGameState gameState) {
        try {
            return objectMapper.writeValueAsString(gameState.getSerializableState());
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar el estado del juego", e);
        }
    }

    /**
     * TODO: Deserializar desde formato de BD a HexGameState.
     * Debe ser compatible con serializeGameState.
     */
    private HexGameState deserializeGameState(String serializedData, String gameId) {
        try {
            Map<String, Object> state = objectMapper.readValue(
                serializedData, new TypeReference<Map<String, Object>>() {}
            );
            int boardSize = 0;
            Object boardSizeObj = state.get("boardSize");
            if (boardSizeObj instanceof Integer) {
                boardSize = (Integer) boardSizeObj;
            } else if (boardSizeObj instanceof Double) {
                boardSize = ((Double) boardSizeObj).intValue();
            } else if (boardSizeObj instanceof String) {
                boardSize = Integer.parseInt((String) boardSizeObj);
            }
            HexGameState gameState = new HexGameState(gameId, boardSize);
            gameState.restoreFromSerializable(state);
            return gameState;
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar el estado del juego", e);
        }
    }

    /**
     * Obtiene estadísticas del repositorio.
     */
    public Map<String, Object> getRepositoryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long totalGames = countWhere(game -> true);
        long finishedGames = countWhere(HexGameState::isGameFinished);
        long wonGames = countWhere(HexGameState::hasPlayerWon);
        stats.put("totalGames", totalGames);
        stats.put("finishedGames", finishedGames);
        stats.put("wonGames", wonGames);
        stats.put("inProgressGames", totalGames - finishedGames);
        stats.put("winRate", totalGames > 0 ? (double) wonGames / totalGames * 100 : 0);
        return stats;
    }

   public long cleanupOldGames(long maxAgeMillis) {
    long currentTime = System.currentTimeMillis();
    return deleteWhere(game -> {
        long gameTime = game.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return currentTime - gameTime > maxAgeMillis;
    });
}
}

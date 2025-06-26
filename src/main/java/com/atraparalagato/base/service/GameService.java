package com.atraparalagato.impl.service;
import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameState.GameStatus;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.strategy.BFSCatMovement;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.repository.H2GameRepository;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Function; 
import com.atraparalagato.base.repository.DataRepository;
/**
 * Implementación esqueleto de GameService para el juego hexagonal.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Orquestación de todos los componentes del juego
 * - Lógica de negocio compleja
 * - Manejo de eventos y callbacks
 * - Validaciones avanzadas
 * - Integración con repositorio y estrategias
 */
public class HexGameService extends GameService<HexPosition> {
    private final H2GameRepository gameRepository;
    private final Supplier<String> gameIdGenerator;
    public HexGameService() {
        // TODO: Los estudiantes deben inyectar las dependencias requeridas
        this(
            new HexGameBoard(11),
            new BFSCatMovement(new HexGameBoard(11)),
            new H2GameRepository(),
            () -> UUID.randomUUID().toString(),
            HexGameBoard::new,
            id -> new HexGameState(id, 11)
        );
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HexGameService(
        HexGameBoard board,
        CatMovementStrategy<HexPosition> movementStrategy,
        H2GameRepository repository,
        Supplier<String> idGenerator,
        Function<Integer, GameBoard<HexPosition>> boardFactory,
        Function<String, GameState<HexPosition>> gameStateFactory
    ) {
        super(
            board,
            movementStrategy,
            (DataRepository) repository,
            idGenerator,
            boardFactory,
            gameStateFactory
        );
        this.gameRepository = repository;
        this.gameIdGenerator = idGenerator;
    }

    /**
     * TODO: Crear un nuevo juego con configuración personalizada.
     * Debe ser más sofisticado que ExampleGameService.
     */
    public HexGameState createGame(int boardSize, String difficulty, Map<String, Object> options) {
        // TODO: Implementar creación de juego avanzada
        if (boardSize < 3) throw new IllegalArgumentException("El tamaño mínimo es 3");
        String gameId = gameIdGenerator.get();
        HexGameBoard board = new HexGameBoard(boardSize);
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, board);
        HexGameState gameState = new HexGameState(gameId, boardSize);
        gameState.setBoard(board);
        gameState.setDifficulty(difficulty);
        gameState.setCatMovementStrategy(strategy);
        gameState.setOnStateChanged(this::onGameStateChanged);
        gameState.setOnGameEnded(this::onGameEnded);
        gameRepository.save(gameState);
        return gameState;
    }

    /**
     * TODO: Ejecutar movimiento del jugador con validaciones avanzadas.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
        // TODO: Implementar movimiento del jugador
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        if (state.isGameFinished()) return Optional.of(state);
        state.makeMove(position, playerId);
        // el gato se mueve estrategicamente
        executeCatMove(state, state.getDifficulty());
        //refresh del mapa y guardar el estado
        gameRepository.save(state);
        notifyGameEvent(gameId, "playerMove", Map.of("position", position, "playerId", playerId));
        return Optional.of(state);
    }

    /**
     * TODO: Obtener estado del juego con información enriquecida.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        // TODO: Obtener estado enriquecido del juego
        return gameRepository.findById(gameId).map(state -> {
            Map<String, Object> map = new HashMap<>();
            map.put("gameState", state);
            map.put("statistics", getGameStatistics(gameId));
            map.put("suggestedMove", getSuggestedMove(gameId).orElse(null));
            map.put("board", state.getBoard());
            map.put("moveHistory", state.getMoveHistory());
            return map;
        });
    }

    /**
     * TODO: Obtener sugerencia inteligente de movimiento.
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        // TODO: Generar sugerencia inteligente
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        List<HexPosition> candidates = state.getBoard().getAllAvailablePositions();
        return candidates.stream()
                .max(Comparator.comparing(pos ->
                        state.getBoard().getAllBorderPositions().stream()
                                .mapToDouble(border -> pos.distanceTo(border))
                                .min()
                                .orElse(Double.MAX_VALUE)
                ));
    }

    /**
     * TODO: Analizar la partida y generar reporte.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        // TODO: Generar análisis completo de la partida
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        Map<String, Object> analysis = new HashMap<>();
        optState.ifPresent(state -> {
            analysis.put("moves", state.getMoveHistory());
            analysis.put("winner", state.getWinner());
            analysis.put("totalMoves", state.getMoveHistory().size());
            analysis.put("difficulty", state.getDifficulty());
        });
        return analysis;
    }

    /**
     * TODO: Obtener estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        // TODO: Calcular estadísticas del jugador
        long total = gameRepository.countWhere(g -> g.getPlayerId().equals(playerId));
        long won = gameRepository.countWhere(g -> g.getPlayerId().equals(playerId) && g.hasPlayerWon());
        Map<String, Object> stats = new HashMap<>();
        stats.put("gamesPlayed", total);
        stats.put("gamesWon", won);
        stats.put("winRate", total > 0 ? (double) won / total * 100 : 0);
        return stats;
    }

    /**
     * TODO: Configurar dificultad del juego.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        // TODO: Cambiar dificultad del juego
        gameRepository.findById(gameId).ifPresent(state -> {
            state.setDifficulty(difficulty);
            state.setCatMovementStrategy(createMovementStrategy(difficulty, state.getBoard()));
            gameRepository.save(state);
        });
    }

    /**
     * TODO: Pausar/reanudar juego.
     */
    public boolean toggleGamePause(String gameId) {
        // TODO: Manejar pausa del juego
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return false;
        HexGameState state = optState.get();
        state.setPaused(!state.isPaused());
        gameRepository.save(state);
        notifyGameEvent(gameId, "pauseToggled", Map.of("paused", state.isPaused()));
        return state.isPaused();
    }

    /**
     * TODO: Deshacer último movimiento.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        // TODO: Implementar funcionalidad de deshacer
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        if (!state.canUndo()) return Optional.of(state);
        state.undoLastMove();
        gameRepository.save(state);
        notifyGameEvent(gameId, "undo", Map.of());
        return Optional.of(state);
    }

    /**
     * TODO: Obtener ranking de mejores puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        // TODO: Generar tabla de líderes
        return gameRepository.findAllSorted(HexGameState::getScore, false).stream()
                .limit(limit)
                .map(state -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("playerId", state.getPlayerId());
                    entry.put("score", state.getScore());
                    entry.put("date", state.getCreatedAt());
                    entry.put("moves", state.getMoveHistory().size());
                    return entry;
                }).toList();
    }

    // Métodos auxiliares que los estudiantes pueden implementar
    private boolean isCatTrapped(HexGameState gameState) {
        HexPosition cat = gameState.getCatPosition();
        List<HexPosition> adj = gameState.getBoard().getAdjacentPositions(cat);
        return adj.isEmpty() || adj.stream().allMatch(gameState.getBoard()::isBlocked);
    }

    private boolean isCatAtBorder(HexGameState gameState) {
        HexPosition cat = gameState.getCatPosition();
        int size = gameState.getBoardSize();
        int q = cat.getQ(), r = cat.getR(), s = cat.getS();
        return Math.abs(q) == size || Math.abs(r) == size || Math.abs(s) == size;
    }

    /**
     * TODO: Validar movimiento según reglas avanzadas.
     */
    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        return !gameState.getBoard().isBlocked(position)
                && !gameState.isGameFinished()
                && !position.equals(gameState.getCatPosition());
    }

    /**
     * TODO: Ejecutar movimiento del gato usando estrategia apropiada.
     */
    private void executeCatMove(HexGameState gameState, String difficulty) {
        Object strategy = gameState.getCatMovementStrategy();
        HexPosition catPos = gameState.getCatPosition();
        List<HexPosition> possibleMoves;
        Optional<HexPosition> move;
    if (isCatTrapped(gameState)) {
        gameState.setGameStatus(GameStatus.PLAYER_WON); 
    } else if (isCatAtBorder(gameState)) {
        gameState.setGameStatus(GameStatus.PLAYER_LOST);
    }
        if (strategy == null) {
        System.out.println("⚠️ catMovementStrategy es null, el gato no se moverá.");
        return;
    }
        if (strategy instanceof BFSCatMovement bfs) {
            possibleMoves = bfs.getPossibleMoves(catPos);
            move = bfs.selectBestMove(possibleMoves, catPos, getTargetPosition(gameState));
        } else if (strategy instanceof AStarCatMovement astar) {
            possibleMoves = astar.getPossibleMoves(catPos);
            move = astar.selectBestMove(possibleMoves, catPos, getTargetPosition(gameState));
        } else {
            possibleMoves = new ArrayList<>();
            move = Optional.empty();
        }
        System.out.println("Posición del gato antes de mover: " + catPos);
        move.ifPresent(gameState::setCatPosition);
        System.out.println("Posición del gato después de mover: " + gameState.getCatPosition());
    }

    /**
     * TODO: Calcular puntuación avanzada.
     */
    @SuppressWarnings("unused")
    private int calculateAdvancedScore(HexGameState gameState, Map<String, Object> factors) {
        int base = gameState.getMoveHistory().size();
        int diff = "hard".equalsIgnoreCase(gameState.getDifficulty()) ? 10 : 0;
        return base + diff;
    }

    /**
     * TODO: Notificar eventos del juego.
     */
    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> eventData) {
        System.out.printf("Evento [%s] en juego %s: %s%n", eventType, gameId, eventData);
    }

    /**
     * TODO: Crear factory de estrategias según dificultad.
     */
    private CatMovementStrategy<HexPosition> createMovementStrategy(String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        } else {
            return new BFSCatMovement(board);
        }
    }

    @Override
    public void onGameStateChanged(GameState<HexPosition> gameState) {
        System.out.println("📊 Estado del juego actualizado: " + gameState.getStatus());
    }
    @Override
    public void onGameEnded(GameState<HexPosition> gameState) {
        String result = gameState.hasPlayerWon() ? "¡VICTORIA!" : "Derrota";
        System.out.println("🏁 Juego terminado: " + result + " - Puntuación: " + gameState.calculateScore());
    }

    // Métodos abstractos requeridos por GameService
    @Override
    public void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> gameBoard) {
        ((HexGameState) gameState).setBoard((HexGameBoard) gameBoard);
    }
    @Override
    public boolean isValidMove(String gameId, HexPosition position) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        return optState.filter(state -> isValidAdvancedMove(state, position, state.getPlayerId())).isPresent();
    }
    @Override
    public Optional<HexPosition> getSuggestedMove(String gameId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        Object strategy = state.getCatMovementStrategy();
        HexPosition catPos = state.getCatPosition();
        if (strategy instanceof BFSCatMovement bfs) {
            return bfs.getPossibleMoves(catPos).stream().findFirst();
        } else if (strategy instanceof AStarCatMovement astar) {
            return astar.getPossibleMoves(catPos).stream().findFirst();
        } else {
            return Optional.empty();
        }
    }
    @Override
    public HexPosition getTargetPosition(GameState<HexPosition> gameState) {
        HexGameBoard board = ((HexGameState) gameState).getBoard();
        HexPosition catPos = ((HexGameState) gameState).getCatPosition();
        return board.getAllBorderPositions().stream()
                .min(Comparator.comparingDouble(catPos::distanceTo))
                .orElse(catPos);
    }
    @Override
    public Object getGameStatistics(String gameId) {
        return gameRepository.findById(gameId)
                .map(GameState::getSerializableState)
                .orElse(null);
    }
}

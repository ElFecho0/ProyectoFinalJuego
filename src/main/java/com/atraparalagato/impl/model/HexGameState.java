package com.atraparalagato.impl.model;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.strategy.BFSCatMovement;
import java.time.Instant;
import java.util.*;
/**
 * Implementación esqueleto de GameState para tableros hexagonales.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Estado del juego más sofisticado que ExampleGameState
 * - Sistema de puntuación avanzado
 * - Lógica de victoria/derrota más compleja
 * - Serialización eficiente
 * - Manejo de eventos y callbacks
 */
public class HexGameState extends GameState<HexPosition> {
    private HexPosition catPosition;
    private final HexGameBoard gameBoard;
    private final int boardSize;
    // HexGameService funciones complementarias
    private String difficulty;
    private CatMovementStrategy<HexPosition> catMovementStrategy;
    private boolean paused = false;
    private String winner;
    private String playerId;
    private final List<Map<String, Object>> moveHistory = new ArrayList<>();
    private final long createdAt = System.currentTimeMillis();
    // TODO: Los estudiantes pueden agregar más campos según necesiten
    // Ejemplos: tiempo de juego, dificultad, power-ups, etc.

    public HexGameState(String gameId, int boardSize) {
        // TODO: Inicializar el tablero y posición inicial del gato
        // init del juego, tablero, identificador
        super(gameId);
        this.boardSize = boardSize;
        this.gameBoard = new HexGameBoard(boardSize);
        this.catPosition = new HexPosition(0, 0);
        this.catMovementStrategy = new BFSCatMovement(gameBoard); 
    }

    //seter
    public void setGameStatus(GameStatus status) {
        super.setStatus(status);
    }

    @Override
    protected boolean canExecuteMove(HexPosition position) {
        // TODO: Implementar validación de movimientos más sofisticada
        // filtro para ejecutar movimientos validos y hacia lugares que no esten bloqueados
        return gameBoard.isValidMove(position);
    }

    @Override
    protected boolean performMove(HexPosition position) {
        // TODO: Ejecutar el movimiento en el tablero
        //simplemente ejecuta el movimiento en el caso de ser valiudado, después se actualizará el tablero
        if (canExecuteMove(position)) {
            gameBoard.blockPosition(position);
            updateGameStatus();
            return true;
        }
        updateGameStatus();
        return false;
    }

    @Override
    protected void updateGameStatus() {
        // TODO: Implementar lógica de determinación de estado del juego
        // esta funcion "escucha" siempre la partida, determina si alguien gano, perdió o hubo un empate
        if (isCatAtBorder()) {
            setStatus(GameStatus.PLAYER_LOST);
            winner = "cat";
        } else if (isCatTrapped()) {
            setStatus(GameStatus.PLAYER_WON);
            winner = playerId != null ? playerId : "player";
        } else if (getMoveCount() >= boardSize * 3) {
            setStatus(GameStatus.DRAW); 
            winner = "draw";
        }
    }

    @Override
    public HexPosition getCatPosition() {
        // TODO: Retornar la posición actual del gato
        // se explica solo, muestra la poscicion del gato en cualquier momento de la partida
        return catPosition;
    }

    @Override
    public void setCatPosition(HexPosition position) {
        // TODO: Establecer la nueva posición del gato
        // se muestra la nueva posicion del gato, de actualiza el tablero y se verifica que haya o no ganador o perdedor del juego
        this.catPosition = position;
        updateGameStatus();
    }

    @Override
    public boolean isGameFinished() {
        // TODO: Verificar si el juego ha terminado
        // si el juego terminó
        return getStatus() != GameStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasPlayerWon() {
        // TODO: Verificar si el jugador ganó
        // si el jugador ganó
        return getStatus() == GameStatus.PLAYER_WON;
    }

    @Override
    public int calculateScore() {
        // TODO: Implementar sistema de puntuación más sofisticado que ExampleGameState
        // calcula el puntaje del jugador
        int baseScore = 0;
        if (hasPlayerWon()) {
            baseScore = 1000 + (boardSize * 50);
            baseScore += Math.max(0, 500 - getMoveCount() * 15);
            List<HexPosition> adj = gameBoard.getAdjacentPositions(catPosition);
            long blockedAdj = adj.stream().filter(gameBoard::isBlocked).count();
            if (blockedAdj >= 4) baseScore += 100;
        } else if (getStatus() == GameStatus.DRAW) {
            baseScore = 200;
        } else {
            baseScore = Math.max(0, 100 - getMoveCount() * 5);
        }
        return baseScore;
    }

    @Override
    public Object getSerializableState() {
        // TODO: Crear representación serializable del estado
        // retorna del juego su estado serializable
        Map<String, Object> state = new HashMap<>();
        state.put("gameId", getGameId());
        state.put("catPosition", Map.of("q", catPosition.getQ(), "r", catPosition.getR()));
        state.put("blockedCells", gameBoard.getBlockedHexPositions());
        state.put("status", getStatus().toString());
        state.put("moveCount", getMoveCount());
        state.put("boardSize", boardSize);
        state.put("createdAt", createdAt);
        return state;
    }

    @Override
    public void restoreFromSerializable(Object serializedState) {
        // TODO: Restaurar el estado desde una representación serializada
        // reestablecer el juego agarrandose de un objeto que esté serializado
        if (serializedState instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) serializedState;
            @SuppressWarnings("unchecked")
            Map<String, Integer> catPos = (Map<String, Integer>) state.get("catPosition");
            if (catPos != null) {
                this.catPosition = new HexPosition(catPos.get("q"), catPos.get("r"));
            }
            String difficulty = (String) state.get("difficulty");
            if (difficulty != null) {
                setDifficulty(difficulty);
                setCatMovementStrategy(new BFSCatMovement(gameBoard)); 
            }
            String statusStr = (String) state.get("status");
            if (statusStr != null) {
                setStatus(GameStatus.valueOf(statusStr));
            }
            Object blockedObj = state.get("blockedCells");
            if (blockedObj instanceof Collection<?> blockedList) {
                gameBoard.getBlockedHexPositions().clear();
                for (Object o : blockedList) {
                    if (o instanceof HexPosition pos) {
                        gameBoard.blockPosition(pos);
                    } else if (o instanceof Map<?,?> map) {
                        Object qObj = map.get("q");
                        Object rObj = map.get("r");
                        if (qObj instanceof Number q && rObj instanceof Number r) {
                            gameBoard.blockPosition(new HexPosition(q.intValue(), r.intValue()));
                        }
                    }
                }
            }
        }
    }
    // Métodos auxiliares que los estudiantes pueden implementar
    /**
     * TODO: Verificar si el gato está en el borde del tablero.
     * Los estudiantes deben definir qué constituye "el borde".
     */
    private boolean isCatAtBorder() {
        // gato en el borde = gato gana = gato escapa
        return Math.abs(catPosition.getQ()) == boardSize ||
               Math.abs(catPosition.getR()) == boardSize ||
               Math.abs(catPosition.getS()) == boardSize;
    }

    /**
     * TODO: Verificar si el gato está completamente atrapado.
     * Debe verificar si todas las posiciones adyacentes están bloqueadas.
     */
    private boolean isCatTrapped() {
        //si el gato, después de buscar caminos validos no bloqueados para escapar, no encuentra ninguno, estará atrapado, por ende el jugador gana
        // como consecuencia, el gato esta atrapado
        if (catMovementStrategy instanceof BFSCatMovement bfs) {
            return !bfs.hasPathToBorder(catPosition);
        }
        List<HexPosition> adj = gameBoard.getAdjacentPositions(catPosition);
        return adj.isEmpty() || adj.stream().allMatch(gameBoard::isBlocked);
    }
    
    /**
     * TODO: Calcular estadísticas avanzadas del juego.
     * Puede incluir métricas como eficiencia, estrategia, etc.
     */
    public HexGameBoard getBoard() { return gameBoard; }
    public void setBoard(HexGameBoard board) {
        // no hace nada
    }
    public int getBoardSize() { return boardSize; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public CatMovementStrategy<HexPosition> getCatMovementStrategy() { return catMovementStrategy; }
    public void setCatMovementStrategy(CatMovementStrategy<HexPosition> strategy) { this.catMovementStrategy = strategy; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public String getWinner() { return winner; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getScore() { return calculateScore(); }

    // para comparar con el valor Instant
    public Instant getCreatedAtInstant() {
        return Instant.ofEpochMilli(createdAt);
    }

    // para comparar con el valor long
    public long getCreatedAtMillis() {
        return createdAt;
    }

    // muestra los movimientos recientemente hechos
    @Override
    public int getMoveCount() {
        return super.getMoveCount();
    }

    public void addMoveToHistory(HexPosition pos, String playerId) {
        Map<String, Object> move = new HashMap<>();
        move.put("position", pos);
        move.put("playerId", playerId);
        move.put("moveNumber", getMoveCount());
        moveHistory.add(move);  
    }
    public List<Map<String, Object>> getMoveHistory() { return moveHistory; }

    // deshace un movimiento, LIFO
    public boolean canUndo() { return !moveHistory.isEmpty(); }
    public void undoLastMove() {
        if (!moveHistory.isEmpty()) {
            Map<String, Object> last = moveHistory.remove(moveHistory.size() - 1);
            Object posObj = last.get("position");
            Set<HexPosition> bloqueos = gameBoard.getBlockedHexPositions();
            if (posObj instanceof HexPosition pos) {
                bloqueos.remove(pos);
            } else if (posObj instanceof Map<?,?> map) {
                Object qObj = map.get("q");
                Object rObj = map.get("r");
                if (qObj instanceof Number q && rObj instanceof Number r) {
                    bloqueos.remove(new HexPosition(q.intValue(), r.intValue()));
                }
            }
            decrementMoveCount();
        }
    }

    public void moveCat() {
    // si el gato es atrapado y cumple con las condiciones, el jugador gana
    if (catMovementStrategy != null) {
        Optional<HexPosition> next = catMovementStrategy.findBestMove(catPosition, null);
        if (next.isPresent()) {
            setCatPosition(next.get());
        } else {
            setGameStatus(GameStatus.PLAYER_WON);
            winner = playerId != null ? playerId : "player";
            }
        }
    }

    // -1 movimientos (undo)
    private void decrementMoveCount() {
        if (getMoveCount() > 0) {
            try {
                java.lang.reflect.Field moveCountField = GameState.class.getDeclaredField("moveCount");
                moveCountField.setAccessible(true);
                int current = (int) moveCountField.get(this);
                moveCountField.set(this, Math.max(0, current - 1));
            } catch (Exception e) {
                //registrar error
            }
        }
    }

    public boolean makeMove(HexPosition pos, String playerId) {
        incrementMoveCount();
        boolean result = performMove(pos);
        addMoveToHistory(pos, playerId);
        return result;
    }
}

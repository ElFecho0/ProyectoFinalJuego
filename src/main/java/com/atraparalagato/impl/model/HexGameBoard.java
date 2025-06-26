package com.atraparalagato.impl.model;
import com.atraparalagato.base.model.GameBoard;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/**
 * Implementación esqueleto de GameBoard para tableros hexagonales.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Modularización: Separación de lógica de tablero hexagonal
 * - OOP: Herencia y polimorfismo
 * - Programación Funcional: Uso de Predicate y streams
 */
public class HexGameBoard extends GameBoard<HexPosition> {
    private final Set<HexPosition> validPositions;
    private final int mySize;
    public HexGameBoard(int size) {
        super(size);
        this.mySize = size;
        this.validPositions = new HashSet<>(getAllPossiblePositions());
    }

    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        // TODO: Los estudiantes deben decidir qué estructura de datos usar
        // el equipo usa HashSet por su rapidez y eficiencia, ademas de su ayuda al detectar duplicados
        return new HashSet<>();
    }

    @Override
    public boolean isPositionInBounds(HexPosition position) {
        // TODO: Implementar validación de límites para tablero hexagonal
        // Ve si esta dentro de los limites (bounds)
        return validPositions.contains(position);
    }

    @Override
    protected boolean isValidMove(HexPosition position) {
        // TODO: Combinar validación de límites y estado actual
        // esta dentro de limites && no bloquado = valido
        return isPositionInBounds(position) && !isBlocked(position);
    }

    @Override
    public void executeMove(HexPosition position) {
        // TODO: Actualizar el estado interno del tablero
        // el movimiento se hace y se bloqueala posicion
        blockPosition(position);
        super.onMoveExecuted(position);
    }

    @Override
    public List<HexPosition> getPositionsWhere(Predicate<HexPosition> condition) {
        // TODO: Implementar usando programación funcional
        // Generar todas las posiciones posibles del tablero
        // Filtrar usando el Predicate
        // entrega de lista que cumplen con las condiciones
        return validPositions.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    @Override
    public List<HexPosition> getAdjacentPositions(HexPosition position) {
        // TODO: Obtener las 6 posiciones adyacentes en un tablero hexagonal
        // entrega de todas las posciciones adyacentes a otra en el tablero, en todas las direcciones posibles
        HexPosition[] directions = {
            new HexPosition(1, 0),
            new HexPosition(1, -1),
            new HexPosition(0, -1),
            new HexPosition(-1, 0),
            new HexPosition(-1, 1),
            new HexPosition(0, 1)
        };
        return Arrays.stream(directions)
                .map(dir -> (HexPosition) position.add(dir))
                .filter(this::isPositionInBounds)
                .filter(pos -> !isBlocked(pos))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBlocked(HexPosition position) {
        // TODO: Verificar si una posición está en el conjunto de bloqueadas
        //esta bloqueada esta posicion? verifica verdadero o falso
        return blockedPositions.contains(position);
    }

    private List<HexPosition> getAllPossiblePositions() {
        // TODO: Generar todas las posiciones válidas del tablero
        //genera la lista que indica todas las posiciones posibles en el tablero
        List<HexPosition> positions = new ArrayList<>();
        for (int q = -mySize; q <= mySize; q++) {
            for (int r = -mySize; r <= mySize; r++) {
                int s = -q - r;
                HexPosition pos = new HexPosition(q, r);
                if (Math.abs(q) <= mySize && Math.abs(r) <= mySize && Math.abs(s) <= mySize) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    // Hook method override - ejemplo de extensibilidad
    @Override
    protected void onMoveExecuted(HexPosition position) {
        // TODO: Los estudiantes pueden agregar lógica adicional aquí
        super.onMoveExecuted(position);
    }

    // mas funciones que ayudan al funcionamiento del programa
    public Set<HexPosition> getBlockedHexPositions() {
        return new HashSet<>(blockedPositions);
    }
    public void blockPosition(HexPosition pos) {
        blockedPositions.add(pos);
    }
    public List<HexPosition> getAllAvailablePositions() {
        return validPositions.stream().filter(pos -> !isBlocked(pos)).collect(Collectors.toList());
    }
    public List<HexPosition> getAllBorderPositions() {
        List<HexPosition> borders = new ArrayList<>();
        for (HexPosition pos : validPositions) {
            int s = pos.getS();
            if (Math.abs(pos.getQ()) == mySize || Math.abs(pos.getR()) == mySize || Math.abs(s) == mySize) {
                borders.add(pos);
            }
        }
        return borders;
    }
}

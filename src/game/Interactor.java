package game;

import java.io.PrintWriter;
import java.util.*;

import static game.MoveType.*;
import static game.Utils.*;

public class Interactor {

    public void solve(int testNumber, Scanner scanner, PrintWriter out) {
        GameParameters gameParameters = new GameParameters();
        BestMoveFinder bestMoveFinder = new BestMoveFinder(gameParameters);
        PhantomUpdater phantomUpdater = new PhantomUpdater(gameParameters);

        List<CheckPoint> checkPoints = genCheckPoints(gameParameters);
        IntReader in = new IntReader(scanner);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        Point myBasePosition = myTeamId == 0 ? new Point(0, 0) : new Point(gameParameters.H, gameParameters.W);
        Point enemyBase = Utils.getEnemyBase(myBasePosition, gameParameters);

        int[] lastStunUsed = new int[bustersPerPlayer * 2];
        Arrays.fill(lastStunUsed, -gameParameters.STUN_COOLDOWN - 5);
        int round = 0;
        in.store();
        List<Buster> phantomEnemies = new ArrayList<>();
        List<Ghost> phantomGhosts = new ArrayList<>();
        while (true) {
            List<Buster> allies = new ArrayList<>();
            List<Buster> enemies = new ArrayList<>();
            List<Ghost> ghosts = new ArrayList<>();

            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int y = in.nextInt();
                int x = in.nextInt();
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.

                if (entityType == -1) {
                    ghosts.add(new Ghost(entityId, x, y, state, value));
                } else {
                    Buster buster = buildBuster(entityId, x, y, state, value, lastStunUsed[entityId], round, gameParameters);
                    if (entityType == myTeamId) {
                        allies.add(buster);
                    } else {
                        enemies.add(buster);
                    }
                }
            }
            allies.sort(Comparator.comparing(Buster::getId));
            updateCheckpoints(allies, checkPoints, round, gameParameters);

            phantomEnemies = phantomUpdater.updatePhantomEnemies(allies, phantomEnemies, enemies, enemyBase);
            phantomGhosts = phantomUpdater.updatePhantomGhosts(ghosts, phantomGhosts, allies, phantomEnemies);

            print(ghosts, "Ghosts");
            print(enemies, "Enemies");
            print(allies, "Allies");
            print(phantomEnemies, "Phantom enemies");

            Set<Integer> alreadyStunnedEnemies = new HashSet<>();
            Set<Integer> alreadyBusted = new HashSet<>();
            for (Buster buster : allies) {
                Move move = bestMoveFinder.findBestMove(buster, myBasePosition, allies, phantomEnemies, phantomGhosts, checkPoints, alreadyStunnedEnemies, alreadyBusted);
                if (move.type == STUN) {
                    lastStunUsed[buster.id] = round;
                    alreadyStunnedEnemies.add(move.targetId);
                }
                if (move.type == BUST) {
                    alreadyBusted.add(move.targetId);
                }
                printMove(buster, move);
            }
            round++;
            in.dump();
        }
    }

    private <T> void print(List<T> list, final String message) {
        System.err.println(message + ":");
        for (T enemy : list) {
            System.err.println(enemy);
        }
    }

    private void updateCheckpoints(List<Buster> myBusters, List<CheckPoint> checkPoints, int round, GameParameters gameParameters) {
        for (Buster myBuster : myBusters) {
            for (CheckPoint checkPoint : checkPoints) {
                if (dist(myBuster, checkPoint.p) <= gameParameters.FOG_RANGE) {
                    checkPoint.lastSeen = round;
                }
            }
        }
    }

    private List<CheckPoint> genCheckPoints(GameParameters gameParameters) {
        List<CheckPoint> r = new ArrayList<>();
        int n = 4;
        int m = 6;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (i == 0 && j == 0 || i == n - 1 && j == m - 1) {
                    continue;
                }
                int x = (int) Math.round((double) i * gameParameters.H / (n - 1));
                int y = (int) Math.round((double) j * gameParameters.W / (m - 1));
                r.add(new CheckPoint(new Point(x, y)));
            }
        }
        return r;
    }

    private void printMove(Buster buster, Move move) {
        System.out.println(move.toInteractorString() + " " + getMessage(buster));
    }

    private String getMessage(Buster buster) {
        return buster.remainingStunCooldown == 0 ? "" : ("" + buster.remainingStunCooldown);
    }

    private Buster buildBuster(int id, int x, int y, int state, int value, int lastStunUsed, int round, GameParameters gameParameters) {
        int stunDelta = round - lastStunUsed;
        int remainingStunCooldown = stunDelta < gameParameters.STUN_COOLDOWN ? gameParameters.STUN_COOLDOWN - stunDelta : 0;
        int remainingStunDuration = 0;
        boolean isCarryingGhost = false;
        int ghostId = -1;
        if (state == 1) {
            isCarryingGhost = true;
            ghostId = value;
        } else if (state == 2) {
            remainingStunDuration = value;
        }
        return new Buster(id, x, y, isCarryingGhost, remainingStunDuration, remainingStunCooldown, ghostId);
    }

    static class IntReader {
        private final Scanner in;
        List<Integer> commonInput = new ArrayList<>();
        List<Integer> input = new ArrayList<>();

        IntReader(Scanner in) {
            this.in = in;
        }

        public int nextInt() {
            int r = in.nextInt();
            input.add(r);
            return r;
        }

        public void dump() {
            System.err.println("input dump:");
            for (int v : commonInput) {
                System.err.print(v + " ");
            }
            for (int v : input) {
                System.err.print(v + " ");
            }
            System.err.println();
            input.clear();
        }

        public void store() {
            commonInput.addAll(input);
            input.clear();
        }
    }
}

package utils.gamebus;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public enum GameEventType {

    error,join, quit, guessed, timeExpired, finalTraces, tag, gameLoading, matchStart, matchEnd, task, askTag, showImages, nextRound, points, guessedObject, timerChange, leaderboard, talk, skipTask, saveTraces, getGameInfo, matchInfo, taskAcquired, json, updateList, unknown
}

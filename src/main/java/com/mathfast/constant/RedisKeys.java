package com.mathfast.constant;

public final class RedisKeys {
    
    private RedisKeys() {
        // Restricted instantiation
    }

    public static final String ROOM_STATE = "room:%s:state";
    public static final String ROOM_CODE_MAPPING = "roomCode:%s:roomId";
    public static final String ROOM_PLAYERS = "room:%s:players";
    public static final String ROOM_SCORES = "room:%s:scores";
    public static final String ROOM_LEADERBOARD = "room:%s:leaderboard";
    public static final String ROOM_RESULTS = "room:%s:results";
    public static final String ROOM_NEXT_ROOM = "room:%s:nextRoomId";
    
    public static final String NONCE_KEY = "nonce:%s";
    public static final String ROOM_STATE_JSON = "room:%s"; // Used by SSE for full state caching

    public static String getRoomStateKey(Object roomId) {
        return String.format(ROOM_STATE, roomId);
    }
    
    public static String getRoomCodeMappingKey(String roomCode) {
        return String.format(ROOM_CODE_MAPPING, roomCode);
    }

    public static String getRoomPlayersKey(Object roomId) {
        return String.format(ROOM_PLAYERS, roomId);
    }

    public static String getRoomScoresKey(Object roomId) {
        return String.format(ROOM_SCORES, roomId);
    }

    public static String getRoomLeaderboardKey(Object roomId) {
        return String.format(ROOM_LEADERBOARD, roomId);
    }
    
    public static String getRoomResultsKey(Object roomId) {
        return String.format(ROOM_RESULTS, roomId);
    }

    public static String getRoomNextRoomKey(Object roomId) {
        return String.format(ROOM_NEXT_ROOM, roomId);
    }
    
    public static String getNonceKey(String nonce) {
        return String.format(NONCE_KEY, nonce);
    }
    
    public static String getRoomStateJson(Object roomId) {
        return String.format(ROOM_STATE_JSON, roomId);
    }
}

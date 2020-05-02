/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.matrixeventsender;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class MatrixActionSet {
    

    // Matrix Room Create Action
    private static String MATRIX_CLIENT_ROOM_CREATE = "/_matrix/client/r0/createRoom"; 
    private static String MATRIX_CLIENT_ROOM_CREATE_ACTION ="POST";
    // Matrix Room Alias Actions
    private static String MATRIX_CLIENT_ROOM_ALIAS_UPDATE = "/_matrix/client/r0/directory/room/";
    private static String MATRIX_CLIENT_ROOM_ALIAS_UPDATE_ACTION = "PUT";
    private static String MATRIX_CLIENT_ROOM_ALIAS_GET = "/_matrix/client/r0/directory/room/";
    private static String MATRIX_CLIENT_ROOM_ALIAS_GET_ACTION = "GET";
    private static String MATRIX_CLIENT_ROOM_ALIAS_DELETE = "/_matrix/client/r0/directory/room/";
    private static String MATRIX_CLIENT_ROOM_ALIAS_DELETE_ACTION = "DELETE";
    // Matrix Membership Actions
    private static String MATRIX_CLIENT_ROOM_JOINED = "/_matrix/client/r0/joined_rooms";
    private static String MATRIX_CLIENT_ROOM_JOINED_ACTION = "GET";
    /*
        10.4.2   Joining rooms
            10.4.2.1   POST /_matrix/client/r0/rooms/{roomId}/invite
            10.4.2.2   POST /_matrix/client/r0/rooms/{roomId}/join
            10.4.2.3   POST /_matrix/client/r0/join/{roomIdOrAlias}
        10.4.3   Leaving rooms
            10.4.3.1   POST /_matrix/client/r0/rooms/{roomId}/leave
            10.4.3.2   POST /_matrix/client/r0/rooms/{roomId}/forget
            10.4.3.3   POST /_matrix/client/r0/rooms/{roomId}/kick
        10.4.4   Banning users in a room
            10.4.4.1   POST /_matrix/client/r0/rooms/{roomId}/ban
            10.4.4.2   POST /_matrix/client/r0/rooms/{roomId}/unban
    10.5   Listing rooms
        10.5.1   GET /_matrix/client/r0/directory/list/room/{roomId}
        10.5.2   PUT /_matrix/client/r0/directory/list/room/{roomId}
        10.5.3   GET /_matrix/client/r0/publicRooms
        10.5.4   POST /_matrix/client/r0/publicRooms */

    
    public String getMatrixClientRoomCreate(){
        return(MATRIX_CLIENT_ROOM_CREATE);
    }
    
}

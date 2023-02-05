#pragma once

#include "../Vector/vector.h"
#include "../InsParse/insparse.h"

typedef enum _STATE
{
	NONE,
	PLAYER1,
	PLAYER2,
	DRAW
} SQUARE_STATE, BOARD_STATE, GAME_STATE;

typedef struct _BOARD_MOVE
{
	BOARD_STATE state;
	unsigned int board_id;
	unsigned int square_id;
	struct _BOARD_MOVE* pnext;
} BOARD_MOVE, *PBOARD_MOVE;

typedef struct _BOARD
{
	BOARD_STATE state;
	SQUARE_STATE squares [9];
} BOARD, *PBOARD;

typedef struct _GAME_ENGINE
{
	GAME_TYPE type;
	PVECTOR boards;
	GAME_STATE state;
	char players_name [3][24];
	unsigned int next_pmoving;
	unsigned int next_board_id;
	unsigned int num_moves;
	PBOARD_MOVE board_moves_list;
} GAME_ENGINE, *PGAME_ENGINE;

extern const char* game_status [DRAW + 1];

// engine functions
//
bool showGameBoard( PGAME_ENGINE pgame_engine, unsigned int board_id );
bool markSquareGameBoard( PGAME_ENGINE pgame_engine, unsigned int square_id );
void resetGameBoard( PGAME_ENGINE pgame_engine );
void processGameBoard( PGAME_ENGINE pgame_engine );
void checkGameBoard( PGAME_ENGINE pgame_engine );
void printLastMovesGameBoard( PGAME_ENGINE pgame_engine, unsigned int num_plays );
PBOARD getBoardsBuffer( PGAME_ENGINE pgame_engine );

// board functions
//
void checkSingularBoard( PBOARD pboard );
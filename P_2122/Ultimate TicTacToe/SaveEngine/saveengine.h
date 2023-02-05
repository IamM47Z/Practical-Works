#pragma once

#include "../InsParse/insparse.h"
#include "../SaveEngine/saveengine.h"
#include "../GameEngine/gameengine.h"

typedef struct _SAVE_FILE
{
	int magic_value;
	time_t save_time;
	GAME_TYPE type;
	GAME_STATE state;
	unsigned int next_pmoving;
	char player_names [2][16];
	unsigned int next_board_id;
	unsigned int num_moves;
	BOARD_MOVE board_moves [9 * 9];
} SAVE_FILE, *PSAVE_FILE;

void packSave( PSAVE_FILE psave_file, PGAME_ENGINE pgame_engine );
bool unpackSave( PSAVE_FILE psave_file, PGAME_ENGINE pgame_engine );
bool saveToFileSave( PSAVE_FILE psave_file, char* filename );
bool loadFromFileSave( PSAVE_FILE psave_file, char* filename );
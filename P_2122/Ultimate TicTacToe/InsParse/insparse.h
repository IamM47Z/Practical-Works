#pragma once

typedef enum _INS_TYPE
{
	SAVE,		// save <filename>
	LOAD,		// load <filename>
	TYPE,		// type <multiplayer (mp)/singleplayer (sp)>
	MARK,		// mark <place (1-9)>
	VIEW,		// view
	HISTORY,	// history <num of plays (1-10)>
	RESTART,	// restart
	EXIT,		// exit
	HELP		// help
} INS_TYPE;

typedef enum _GAME_TYPE
{
	SINGLEPLAYER,
	MULTIPLAYER
} GAME_TYPE;

typedef struct _INS_INFO
{
	INS_TYPE type;
	union
	{
		char filename[ 16 ];
		GAME_TYPE new_game_type;
		int place;
		int num_plays;
	};
} INS_INFO, *PINS_INFO;

extern const char* help_messages [HELP + 1];

void showInstructionSyntax( INS_TYPE type );
bool parseInstruction( char* ins_buffer, size_t buffer_size, PINS_INFO pout );
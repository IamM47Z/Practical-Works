#pragma once

#include "../InsParse/insparse.h"
#include "../GameEngine/gameengine.h"

GAME_TYPE getGameType( );
void showErrorMessage( char* str );
bool showConfirmMessage( char* str );
void getPlayerNames( PGAME_ENGINE pgame_engine );
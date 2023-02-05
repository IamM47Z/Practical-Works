#ifndef INSPARSE_H
#define INSPARSE_H

#include "../shared.h"
#include "../terminal/terminal.h"

enum INS_TYPE
{
	ANIMAL,     // animal <specie> <y: optional> <x: optional>
	KILL,       // kill <y> <x>
	KILLID,     // killid <id>
	FOOD,       // foods <type> <y: optional> <x: optional>
	FEED,       // feed <y> <x> <nutritive_points> <toxicity_points>
	FEEDID,     // feedid <id> <nutritive_points> <toxicity_points>
	NOFOOD,     // nofood <y> <x> or <id>
	EMPTY,      // empty <y> <x>
	SEE,        // see <y> <x>
	INFO,       // info <id>
	NEXT,       // n <steps: optional> <delay: optional>
	ANIM,       // anim
	VISANIM,    // visanim
	STORE,      // store <name>
	RESTORE,    // restore <name>
	LOAD,       // load <file-name>
	SLIDE,      // slide <direction> <step>
	EXIT,       // exit
	HELP        // help
};

typedef struct INS_INFO
{
	INS_TYPE type;
	std::vector< std::string > args;
	union
	{
		SPECIE specie;
	};
} * PINS_INFO;

namespace insparse
{
	void showHelp( term::Window& hwnd );
	
	void showInstructionSyntax( term::Window& hwnd, INS_TYPE ins_type );
	
	bool parseInstruction( term::Window& hwnd, const std::string& instruction, INS_INFO& ins_info );
}

#endif //INSPARSE_H

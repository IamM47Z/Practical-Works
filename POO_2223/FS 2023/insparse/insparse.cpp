#include <vector>
#include <sstream>
#include <unistd.h>

#include "insparse.h"
#include "../utils/utils.h"
#include "../terminal/terminal.h"

namespace insparse
{
	namespace
	{
		// all instruction type names
		//
		const char* instruction_types[] = { "ANIMAL", "KILL", "KILLID", "FOOD", "FEED",
		                                    "FEEDID", "NOFOOD", "EMPTY", "SEE", "INFO",
		                                    "NEXT", "ANIM", "VISANIM", "STORE", "RESTORE",
		                                    "LOAD", "SLIDE", "EXIT", "HELP" };
		
		// all instruction help messages
		//
		const char* help_messages[HELP + 1] = {
				"ANIMAL <specie> <y, x: optional> create animal at a possible specified position\n",
				"KILL <y> <x> kills the animal at specified position\n",
				"KILLID <id> kills the animal with <id>\n",
				"FOOD <type> <y, x: optional> places foods of <type> at a possible specified position\n",
				"FEED <y> <x> <nutritive_points> <toxicity_points> feeds the animal at specified position\n",
				"FEEDID <id> <nutritive_points> <toxicity_points> feeds the animal with specified id\n",
				"NOFOOD <id> or <y> <x> removes the foods with specified id or at specified position\n",
				"EMPTY <y> <x> delete entity at specified position\n",
				"SEE <y> <x> get information at specified position\n",
				"INFO <id> get entity information\n",
				"NEXT (alias: N) <ticks: optional> <delay: optional> processes ticks\n",
				"ANIM shows all animalspecies id, specie and health\n",
				"VISANIM shows visible animalspecies id, specie and health\n",
				"STORE <name> saves current reserve as <name>\n",
				"RESTORE <name> restores reserve <name>\n",
				"LOAD <file-name> loads commands from file <file-name>\n",
				"SLIDE <direction> <step> moves camera <step> steps to <direction>\n",
				"EXIT closes the game\n",
				"HELP shows all commands info\n\n" };
	}
	
	void showHelp( term::Window& hwnd )
	{
		for ( auto& help_message: help_messages )
			hwnd << help_message;
	}
	
	void showInstructionSyntax( term::Window& hwnd, INS_TYPE ins_type )
	{
		hwnd << instruction_types[ ins_type ] << "\n\n" << help_messages[ ins_type ] << "\n";
	}
	
	bool parseInstruction( term::Window& hwnd, const std::string& instruction, INS_INFO& ins_info )
	{
		if ( instruction.length( ) < 1 )
			return false;
		
		std::string ins_type;
		std::stringstream ins_stream( instruction );
		if ( !( ins_stream >> ins_type ) )
			return false;
		
		std::string ins_arg;
		while ( ins_stream >> ins_arg )
			ins_info.args.push_back( ins_arg );
		
		auto n_args = ins_info.args.size( );
		
		auto status = true;
		if ( utils::istrcmp( ins_type, "animal" ) )
		{
			ins_info.type = ANIMAL;
			
			if ( n_args != 1 && n_args != 3 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "kill" ) )
		{
			ins_info.type = KILL;
			
			if ( n_args != 2 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "killid" ) )
		{
			ins_info.type = KILLID;
			
			if ( n_args != 1 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "food" ) )
		{
			ins_info.type = FOOD;
			
			if ( n_args != 1 && n_args != 3 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "feed" ) )
		{
			ins_info.type = FEED;
			
			if ( n_args != 4 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "feedid" ) )
		{
			ins_info.type = FEEDID;
			
			if ( n_args != 3 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "nofood" ) )
		{
			ins_info.type = NOFOOD;
			
			if ( n_args != 1 && n_args != 2 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "empty" ) )
		{
			ins_info.type = EMPTY;
			
			if ( n_args != 2 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "see" ) )
		{
			ins_info.type = SEE;
			
			if ( n_args != 2 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "info" ) )
		{
			ins_info.type = INFO;
			
			if ( n_args != 1 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "next" ) || utils::istrcmp( ins_type, "n" ) )
		{
			ins_info.type = NEXT;
			
			if ( n_args > 2 || n_args < 0 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "anim" ) )
		{
			ins_info.type = ANIM;
			
			if ( n_args )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "visanim" ) )
		{
			ins_info.type = VISANIM;
			
			if ( n_args )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "store" ) )
		{
			ins_info.type = STORE;
			
			if ( n_args != 1 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "restore" ) )
		{
			ins_info.type = RESTORE;
			
			if ( n_args != 1 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "load" ) )
		{
			ins_info.type = LOAD;
			
			if ( n_args != 1 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "slide" ) )
		{
			ins_info.type = SLIDE;
			
			if ( n_args != 2 )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "help" ) )
		{
			ins_info.type = HELP;
			
			if ( n_args )
				status = false;
		}
		else if ( utils::istrcmp( ins_type, "exit" ) )
		{
			ins_info.type = EXIT;
			
			if ( n_args )
				status = false;
		}
		else
		{
			hwnd << "Invalid Command\n";
			return false;
		}
		
		// show how to use the attempted instruction in case the type exists
		//
		if ( !status )
			showInstructionSyntax( hwnd, ins_info.type );
		
		return status;
	}
}
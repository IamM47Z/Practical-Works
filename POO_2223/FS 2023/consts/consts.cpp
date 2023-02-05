#include <map>
#include <vector>
#include <sstream>
#include <fstream>

#include "consts.h"

namespace consts
{
	// anonymous namespace
	namespace
	{
		bool is_loaded = false;
		
		std::map< std::string, int > consts_map;
	}
	
	bool getConst( const std::string& const_name, int& const_value )
	{
		if ( !is_loaded || !consts_map.count( const_name ) )
			return false;
		
		const_value = consts_map[ const_name ];
		return true;
	}
	
	bool loadConsts( const std::string& file_path )
	{
		is_loaded = false;
		consts_map.clear( );
		
		// read file
		std::ifstream file( file_path );
		if ( !file.is_open( ) )
			return false;
		
		// read line
		std::string line{ };
		while ( std::getline( file, line ) )
		{
			std::istringstream line_stream( line );
			
			std::string const_name;
			int const_value;
			
			// error handling in case the file is not well formatted
			if ( !( line_stream >> const_name >> const_value ) )
			{
				consts_map.clear( );
				file.close( );
				return false;
			}
			
			consts_map[ const_name ] = const_value;
		}
		
		file.close( );
		is_loaded = true;
		return true;
	}
	
	bool isLoaded( )
	{
		return is_loaded;
	}
}
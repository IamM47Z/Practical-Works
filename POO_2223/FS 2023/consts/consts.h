#ifndef CONSTS_H
#define CONSTS_H

#include <string>

namespace consts
{
	bool loadConsts( const std::string& file_path );
	
	bool getConst( const std::string& const_name, int& const_value );
	
	bool isLoaded( );
}


#endif //CONSTS_H

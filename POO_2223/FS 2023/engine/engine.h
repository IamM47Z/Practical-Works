#ifndef ENGINE_H
#define ENGINE_H

#include "../shared.h"

#include "../reserve/reserve.h"
#include "../entity/foods/foods.h"
#include "../entity/animals/animals.h"

namespace engine
{
	bool initialize( );
	
	Reserve* getReserve( );
	
	void setReserve( Reserve& reserve );
	
	void render( );
	
	bool processKeys( );
}

#endif //ENGINE_H

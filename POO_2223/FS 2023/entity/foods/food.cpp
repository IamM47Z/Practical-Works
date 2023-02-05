#include <iostream>
#include "food.h"
#include "../../utils/utils.h"
#include "../../engine/engine.h"

Food::Food( const std::string& species ) : Entity( )
{
	this->species = species;
}

FoodEntry& Food::getEntry( )
{
	return *dynamic_cast<FoodEntry*>( this );
}

bool Food::addSmell( SMELL smell )
{
	if ( std::count( smells.begin( ), smells.end( ), smell ) )
		return false;
	
	smells.push_back( smell );
	return true;
}

bool Food::moveTo( COORD_PAIR new_coords )
{
	// clamp coords
	const auto reserve = engine::getReserve( );
	new_coords = utils::clampCoords( reserve->getMaxCoords( ), new_coords );
	
	auto exists = false;
	reserve->forEachEntity( [ this, new_coords, &exists ]( const EntityPtr& entity )
	                        {
		                        if ( this->id == entity->getId( ) || !std::dynamic_pointer_cast< Food >( entity ) )
			                        return true;
		
		                        const auto location = entity->getCoords( );
		                        if ( new_coords.first == location.first && new_coords.second == location.second )
		                        {
			                        exists = true;
			                        return false;
		                        }
		
		                        return true;
	                        } );
	if ( !exists )
	{
		coords = new_coords;
		return true;
	}
	
	return false;
}

bool Food::smellsLike( SMELL smell )
{
	return std::count( smells.begin( ), smells.end( ), smell ) != 0;
}
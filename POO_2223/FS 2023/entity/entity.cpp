#include <vector>

#include "entity.h"

#include "../utils/utils.h"
#include "../engine/engine.h"

Entity::Entity( )
{
	static int entity_num = 1;
	id = entity_num;
	entity_num++;
}

Entity::~Entity( )
{
	engine::getReserve( )->removeEntity( id );
}

bool Entity::moveTo( COORD_PAIR new_coords )
{
	// clamp coords
	const auto reserve = engine::getReserve( );
	coords = utils::clampCoords( reserve->getMaxCoords( ), new_coords );
	return true;
}

COORD_PAIR Entity::getCoords( )
{
	return coords;
}

bool Entity::isAlive( ) const
{
	return alive;
}

char Entity::getVisualChar( ) const
{
	return this->visual_representation;
}

int Entity::getId( ) const
{
	return this->id;
}

void Entity::remove( )
{
	if ( !isAlive( ) )
		return;
	
	alive = false;
	onDeath( );
}
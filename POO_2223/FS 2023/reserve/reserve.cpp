#include <thread>
#include <vector>
#include <string>
#include <utility>

#include "reserve.h"
#include "../engine/engine.h"
#include "../utils/utils.h"

Reserve::~Reserve( )
{
	entity_list.clear( );
}

COORD_PAIR Reserve::getMaxCoords( )
{
	return max_coords;
}

void Reserve::processUpdates( )
{
	clearBuffer( );
	
	// remove death and invalid entities
	//
	entity_list.erase( std::remove_if( entity_list.begin( ), entity_list.end( ), [ ]( const EntityPtr& entity )
	{
		return !entity || !entity->isAlive( );
	} ), entity_list.end( ) );
	
	
	for ( auto& entity: entity_list )
	{
		if ( std::dynamic_pointer_cast< Kangaroo >( entity ) )
		{
			const auto kangaroo = std::static_pointer_cast< Kangaroo >( entity );
			if ( kangaroo->isInBag( ) )
				continue;
		}
		
		if ( entity->isAlive( ) )
			this->updateArea( entity->getCoords( ), entity->getVisualChar( ) );
	}
}

void Reserve::processTicks( int nticks, int delay )
{
	auto& hwnd = getConsoleHwnd( );
	while ( nticks )
	{
		hwnd.clear( );
		hwnd.move( 0, 2 );
		
		for ( auto& entity: entity_list )
			if ( entity && entity->isAlive( ) )
				entity->processTick( );
		
		nticks--;
		if ( nticks )
		{
			// remove death and invalid entities
			//
			entity_list.erase( std::remove_if( entity_list.begin( ), entity_list.end( ), [ ]( const EntityPtr& entity )
			{
				return !entity || !entity->isAlive( );
			} ), entity_list.end( ) );
			
			hwnd << "Remaining Ticks: " << nticks << "\n";
			
			engine::render( );
			std::this_thread::sleep_for( std::chrono::seconds( delay ) );
		}
	}
}

EntityPtr Reserve::getEntityById( int id )
{
	for ( auto& entity: entity_list )
		if ( entity->getId( ) == id )
			return entity;
	
	return nullptr;
}

AnimalPtr Reserve::createAnimal( const std::string& specie_initial, COORD_PAIR location )
{
	AnimalPtr panimal = nullptr;
	if ( !location.first && !location.second )
	{
		location.first = utils::genRandomNum(
				1, max_coords.first );
		location.second = utils::genRandomNum(
				1, max_coords.second );
	}
	
	if ( utils::istrcmp( specie_initial, "c" ) )
		panimal = std::make_shared< Rabbit >( );
	else if ( utils::istrcmp( specie_initial, "o" ) )
		panimal = std::make_shared< Sheep >( );
	else if ( utils::istrcmp( specie_initial, "l" ) )
		panimal = std::make_shared< Wolf >( );
	else if ( utils::istrcmp( specie_initial, "g" ) )
		panimal = std::make_shared< Kangaroo >( );
	else if ( utils::istrcmp( specie_initial, "m" ) )
		panimal = std::make_shared< Mistery >( );
	else
		return nullptr;
	
	while ( !panimal->moveTo( location ) )
	{
		location.first = utils::genRandomNum( 1, max_coords.first );
		location.second = utils::genRandomNum( 1, max_coords.second );
	}
	
	if ( !addEntity( panimal ) )
	{
		panimal.reset( );
		return nullptr;
	}
	
	return panimal;
}

void Reserve::killAnimal( const AnimalPtr& panimal )
{
	if ( !panimal && !panimal->isAlive( ) )
		return;
	
	panimal->remove( );
	this->removeEntity( panimal->getId( ) );
}

FoodPtr Reserve::growFood( const std::string& specie_initial, std::pair< int, int > location )
{
	FoodPtr pfood = nullptr;
	auto is_random = !location.first && !location.second;
	if ( is_random )
	{
		location.first = utils::genRandomNum(
				1, max_coords.first );
		location.second = utils::genRandomNum(
				1, max_coords.second );
	}
	
	if ( utils::istrcmp( specie_initial, "r" ) )
		pfood = std::make_shared< Grass >( );
	else if ( utils::istrcmp( specie_initial, "t" ) )
		pfood = std::make_shared< Carrot >( );
	else if ( utils::istrcmp( specie_initial, "b" ) )
		pfood = std::make_shared< Beef >( );
	else if ( utils::istrcmp( specie_initial, "a" ) )
		pfood = std::make_shared< MisteryFood >( );
	else
		return nullptr;
	
	if ( !pfood->moveTo( location ) )
	{
		if ( !is_random )
		{
			pfood.reset( );
			return nullptr;
		}
		
		do
		{
			location.first = utils::genRandomNum( 1, max_coords.first );
			location.second = utils::genRandomNum( 1, max_coords.second );
		}
		while ( !pfood->moveTo( location ) );
	}
	
	if ( !addEntity( pfood ) )
	{
		pfood.reset( );
		return nullptr;
	}
	
	return pfood;
}

void Reserve::removeFood( const FoodPtr& pfood )
{
	if ( !pfood )
		return;
	
	pfood->remove( );
	this->removeEntity( pfood->getId( ) );
}

void Reserve::forEachEntity( const std::function< bool( EntityPtr ) >& callback )
{
	for ( auto& entity: entity_list )
		if ( !callback( entity ) )
			break;
}

void Reserve::removeEntity( int entity_id )
{
	entity_list.erase( std::remove_if( entity_list.begin( ), entity_list.end( ), [ entity_id ]( EntityPtr& pentity )
	{
		return !pentity || pentity->getId( ) == entity_id;
	} ), entity_list.end( ) );
}

bool Reserve::addEntity( const EntityPtr& pentity )
{
	if ( std::count( entity_list.begin( ), entity_list.end( ), pentity ) == 0 )
	{
		entity_list.push_back( pentity );
		return true;
	}
	
	return false;
}

Reserve Reserve::getClone( )
{
	Reserve reserve( max_coords.first, max_coords.second );
	
	for ( auto& entity: entity_list )
		reserve.addEntity( entity->getClone( ) );
	
	return reserve;
}
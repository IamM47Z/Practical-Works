#include <cmath>
#include "kangaroo.h"

#include "../../utils/utils.h"
#include "../../consts/consts.h"
#include "../../engine/engine.h"
#include "../foods/corpses.h"

Kangaroo::Kangaroo( ) : Animal( "Canguru" )
{
	visual_representation = 'G';
	
	if ( !consts::getConst( "SCanguru", health ) || !consts::getConst( "VCanguru", est_age ) )
	{
		endwin( );
		perror( "Constants could not be found" );
		exit( 1 );
	}
	
	range = 7;
	weight = 10;
}

void Kangaroo::move( DIRECTION direction )
{
	auto npos_per_tick = ( hunger > 15 ) ? 2 : 1;
	
	const auto ptr_parent = engine::getReserve( )->getEntityById( parent_id );
	if ( age < 10 && ptr_parent )
	{
		if ( afraid )
		{
			npos_per_tick *= 2;
			
			const auto parent_location = ptr_parent->getCoords( );
			const auto diff_x = coords.first - parent_location.first;
			const auto diff_y = coords.second - parent_location.second;
			if ( diff_x + diff_y <= npos_per_tick )
			{
				this->moveTo( parent_location );
				return;
			}
			
			direction = utils::getTargetDirection( coords, parent_location );
		}
		else
		{
			auto child_coords = ptr_parent->getCoords( );
			child_coords.first += utils::genRandomNum( -15, 15 );
			child_coords.second += utils::genRandomNum( -15, 15 );
			this->moveTo( child_coords );
			return;
		}
	}
	
	auto coords = this->coords;
	if ( direction.up )
		coords.second -= npos_per_tick;
	if ( direction.down )
		coords.second += npos_per_tick;
	if ( direction.left )
		coords.first -= npos_per_tick;
	if ( direction.right )
		coords.first += npos_per_tick;
	
	this->moveTo( coords );
	for ( auto& child: childs_in_bag )
		child->moveTo( coords );
}

void Kangaroo::processTick( )
{
	age++;
	
	if ( in_bag == 5 )
		in_bag = 0;
	else if ( in_bag != 0 )
	{
		in_bag++;
		return;
	}
	
	if ( age == 20 )
		weight = 20;
	
	if ( health <= 0 )
		this->remove( );
	
	if ( !( age % 30 ) )
		this->reproduce( );
	
	const auto ptr_reserve = engine::getReserve( );
	const auto ptr_parent = ptr_reserve->getEntityById( parent_id );
	
	if ( afraid && ptr_parent && ptr_parent->getCoords( ) == coords )
	{
		in_bag = 1;
		return;
	}
	
	// update childs in bag
	childs_in_bag.clear( );
	for ( auto& id: childs_id )
	{
		const auto child = std::dynamic_pointer_cast< Kangaroo >( ptr_reserve->getEntityById( id ) );
		if ( !child || child->isInBag( ) )
			continue;
		
		childs_in_bag.emplace_back( child );
	}
	
	std::vector< AnimalPtr > nearby_animals_vec;
	ptr_reserve->forEachEntity( [ this, &nearby_animals_vec ]( const EntityPtr& entity )
	                            {
		                            if ( this->id == entity->getId( ) )
			                            return true;
		
		                            const auto location = entity->getCoords( );
		                            if ( this->coords.first - this->range > location.first ||
		                                 location.first > this->coords.first + this->range ||
		                                 this->coords.second - this->range > location.second ||
		                                 location.second > this->coords.second + this->range ||
		                                 this->coords.first == location.first ||
		                                 this->coords.second == location.second )
			                            return true;
		
		                            if ( std::dynamic_pointer_cast< Animal >( entity ) )
			                            nearby_animals_vec.emplace_back( std::static_pointer_cast< Animal >( entity ) );
		
		                            return true;
	                            } );
	
	DIRECTION dir{ };
	// if its a child
	if ( age < 10 && ptr_parent )
		afraid = !nearby_animals_vec.empty( );
	else
	{
		// detect threats
		float max_weight = 10.0f;
		for ( auto& animal: nearby_animals_vec )
		{
			if ( animal->getWeight( ) <= max_weight )
				continue;
			
			// we just found a bigger threat, lets run the oposite way
			dir = utils::getTargetDirection( coords, animal->getCoords( ), true );
			max_weight = animal->getWeight( );
		}
		
		if ( dir.value )
			return this->move( dir );
		
		// generate a random direction
		switch ( utils::genRandomNum( 0, 7 ) )
		{
			case 0:
				dir.up = true;
				break;
			case 1:
				dir.left = true;
				break;
			case 2:
				dir.right = true;
				break;
			case 3:
				dir.down = true;
				break;
			case 4:
				dir.up = true;
				dir.left = true;
				break;
			case 5:
				dir.up = true;
				dir.right = true;
				break;
			case 6:
				dir.down = true;
				dir.left = true;
				break;
			case 7:
				dir.down = true;
				dir.right = true;
				break;
			default:
				break;
		}
	}
	
	// move
	this->move( dir );
}

void Kangaroo::reproduce( )
{
	auto child_coords = this->coords;
	child_coords.first += utils::genRandomNum( -3, 3 );
	child_coords.second += utils::genRandomNum( -3, 3 );
	
	auto pchild = std::make_shared< Kangaroo >( );
	if ( !pchild->moveTo( child_coords ) )
		return;
	
	pchild->parent_id = id;
	
	if ( !engine::getReserve( )->addEntity( pchild ) )
	{
		endwin( );
		perror( "Fatal error Adding Entity to Entity List" );
		exit( 1 );
	}
	
	if ( !addChild( pchild->getId( ) ) )
	{
		endwin( );
		perror( "Fatal error Adding Child to Child's List" );
		exit( 1 );
	}
}

void Kangaroo::onDeath( )
{
	const auto ptr_reserve = engine::getReserve( );
	for ( auto& child: childs_in_bag )
		child->remove( );
	
	auto pcorpses = std::make_shared< Corpses >( 15, 5 );
	if ( !pcorpses->moveTo( this->coords ) )
	{
		ptr_reserve->getConsoleHwnd( ) << "There is already food at (" << this->coords.first << ", "
		                               << this->coords.second << ")\n";
		return;
	}
	
	if ( !ptr_reserve->addEntity( pcorpses ) )
	{
		endwin( );
		perror( "Fatal error Adding Entity to Entity List" );
		exit( 1 );
	}
}

bool Kangaroo::isInBag( ) const
{
	return in_bag > 0;
}

std::shared_ptr< Entity > Kangaroo::getClone( )
{
	auto pclone = std::make_shared< Kangaroo >( );
	
	// entity class
	//
	pclone->id = id;
	pclone->alive = alive;
	
	pclone->range = range;
	pclone->visual_representation = visual_representation;
	pclone->coords = coords;
	
	// animal class
	//
	pclone->parent_id = parent_id;
	
	pclone->species = species;
	
	pclone->age = age;
	pclone->est_age = est_age;
	pclone->childs_id = childs_id;
	
	pclone->health = health;
	pclone->hunger = hunger;
	pclone->weight = weight;
	
	pclone->history = history;
	pclone->taste = taste;
	
	// kangaroo class
	//
	pclone->in_bag = in_bag;
	pclone->afraid = afraid;
	pclone->childs_in_bag.clear( );
	
	return pclone;
}
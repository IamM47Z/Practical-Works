#include "sheep.h"

#include <utility>

#include "../foods/corpses.h"

#include "../../utils/utils.h"
#include "../../consts/consts.h"
#include "../../engine/engine.h"

Sheep::Sheep( ) : Animal( "Ovelha" )
{
	visual_representation = 'O';
	
	if ( !consts::getConst( "SOvelha", health ) || !consts::getConst( "VOvelha", est_age ) )
	{
		endwin( );
		perror( "Constants could not be found" );
		exit( 1 );
	}
	
	range = 3;
	taste.emplace_back( ERVA );
	weight = static_cast<float>( utils::genRandomNum( 4, 8 ) );
}

void Sheep::move( DIRECTION direction )
{
	auto npos_per_tick = 1;
	if ( hunger > 15 )
		npos_per_tick = utils::genRandomNum( 1, 2 );
	
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
}

void Sheep::processTick( )
{
	age++;
	hunger++;
	
	if ( hunger > 20 )
		health -= 2;
	else if ( hunger > 15 )
		health--;
	
	if ( age == est_age || health <= 0 )
		this->remove( );
	
	if ( !( age % 15 ) )
		this->reproduce( );
	
	const auto ptr_reserve = engine::getReserve( );
	
	FoodPtr ptr_food = nullptr, ptr_nearby_food = nullptr;
	std::vector< AnimalPtr > nearby_animals_vec;
	ptr_reserve->forEachEntity( [ this, &ptr_food, &ptr_nearby_food, &nearby_animals_vec ]( const EntityPtr& entity )
	                            {
		                            if ( this->id == entity->getId( ) )
			                            return true;
		
		                            const auto location = entity->getCoords( );
		                            if ( this->coords.first - this->range > location.first ||
		                                 location.first > this->coords.first + this->range ||
		                                 this->coords.second - this->range > location.second ||
		                                 location.second > this->coords.second + this->range )
			                            return true;
		
		                            if ( std::dynamic_pointer_cast< Food >( entity ) )
		                            {
			                            const auto food = std::static_pointer_cast< Food >( entity );
			
			                            auto enjoyable = false;
			                            for ( auto& smell: this->taste )
				                            if ( food->smellsLike( smell ) )
				                            {
					                            enjoyable = true;
					                            break;
				                            }
			
			                            if ( !enjoyable )
				                            return true;
			
			                            if ( this->coords.first == location.first &&
			                                 this->coords.second == location.second )
				                            ptr_food = food;
			                            else if ( !ptr_nearby_food )
				                            ptr_nearby_food = food;
			                            else
			                            {
				                            const auto toxicity_diff =
						                            ptr_nearby_food->getToxicity( ) - food->getToxicity( );
				                            const auto nutritive_diff =
						                            ptr_nearby_food->getNutritive( ) - food->getNutritive( );
				
				                            if ( toxicity_diff > 0 && nutritive_diff < 0 )
					                            ptr_nearby_food = food;
				                            else if ( toxicity_diff == 0 && nutritive_diff == 0 )
				                            {
					                            const auto diff_x_nearby =
							                            ptr_nearby_food->getCoords( ).first - this->coords.first;
					                            const auto diff_y_nearby =
							                            ptr_nearby_food->getCoords( ).second - this->coords.second;
					                            const auto diff_x_current =
							                            food->getCoords( ).first - this->coords.first;
					                            const auto diff_y_current =
							                            food->getCoords( ).second - this->coords.second;
					
					                            if ( diff_x_nearby * diff_x_nearby + diff_y_nearby * diff_y_nearby >
					                                 diff_x_current * diff_x_current + diff_y_current * diff_y_current )
						                            ptr_nearby_food = food;
				                            }
			                            }
			
			                            return true;
		                            }
		
		                            if ( std::dynamic_pointer_cast< Animal >( entity ) )
			                            nearby_animals_vec.emplace_back( std::static_pointer_cast< Animal >( entity ) );
		
		                            return true;
	                            } );
	
	// eat if food is here
	if ( ptr_food )
	{
		eat( ptr_food->getEntry( ) );
		ptr_food->remove( );
	}
	
	// detect threats
	DIRECTION dir{ };
	float max_weight = 15.0f;
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
	
	// get nearby food direction or generate a random one
	if ( ptr_nearby_food )
		dir = utils::getTargetDirection( coords, ptr_nearby_food->getCoords( ) );
	else
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
	
	// move
	this->move( dir );
}

void Sheep::reproduce( )
{
	auto child_coords = this->coords;
	child_coords.first += utils::genRandomNum( -12, 12 );
	child_coords.second += utils::genRandomNum( -12, 12 );
	
	auto pchild = std::make_shared< Sheep >( );
	if ( !pchild->moveTo( child_coords ) )
		return;
	
	pchild->parent_id = id;
	pchild->health = health;
	
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

void Sheep::onDeath( )
{
	auto pcorpses = std::make_shared< Corpses >( weight, 0 );
	if ( !pcorpses->moveTo( this->coords ) )
	{
		engine::getReserve( )->getConsoleHwnd( ) << "There is already food at (" << this->coords.first << ", "
		                                         << this->coords.second << ")\n";
		return;
	}
	
	if ( !engine::getReserve( )->addEntity( pcorpses ) )
	{
		endwin( );
		perror( "Fatal error Adding Entity to Entity List" );
		exit( 1 );
	}
}

std::shared_ptr< Entity > Sheep::getClone( )
{
	auto pclone = std::make_shared< Sheep >( );
	
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
	
	return pclone;
}
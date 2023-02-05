import React from 'react';
import './gameboard.css';

import LetterSquare  from '../lettersquare/lettersquare.component';

class GameBoard extends React.Component
{
    render( ) 
    {
        return (
            <div className={ "gameboard " + this.props.level_str }>
                { 
                    this.props.letters_array.map(
                        ( letter, index ) => {
                            return ( <LetterSquare letter={ letter } reset_board={ this.props.reset_board } id={ index } key={ index }/> );
                        }
                    )
                }
            </div>
        );
    }
}

export default GameBoard;
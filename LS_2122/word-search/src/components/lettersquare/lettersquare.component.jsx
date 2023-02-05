import React from 'react';
import ReactDOM from 'react-dom';

import './lettersquare.css';

class LetterSquare extends React.Component
{
    componentDidUpdate( prev )
    {
        if ( prev.reset_board === this.props.reset_board || !this.props.reset_board )
            return;

        let elem = ReactDOM.findDOMNode(this);
        elem.classList.remove( "active" );
        elem.classList.remove( "valid" );

        console.log( "reseting\n" );
    }

    render( ) 
    {
        return (
            <button type='button' className='btn btn-primary btn-square letter-square' id={ this.props.id } >
                { this.props.letter }
            </button>
        );
    }
}

export default LetterSquare;
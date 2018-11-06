import React, { Component } from 'react';

import './CardView.css';

class CardView extends Component {

    constructor() {
        super();
    }

    render() {
        const data = this.props.dataObject;
        return (
            
            <div className="cardView">
                <div className="cardView__header">
                    <p>{data.id} </p>
                    <h4>{data.date.toDate().toLocaleString()}</h4>
                </div>
                <div className="cardView__content">
                    <p>LDSA: {data.ldsa} </p>
                    <p>Temp: {data.temp}</p>
                    <p>Humidity: {data.humidity}</p>
                    <p>Batt. Volt.: {data.batteryVoltage}</p>
                    <p>Error Msg.: {data.error}</p>
                    <p>Diameter: {data.diameter}</p>
                    <p>Humidity: {data.humidity}</p>
                </div>
            </div>
        )
    }
}

export default CardView;
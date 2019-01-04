import React, { Component } from 'react';
import firebase from "./Firestore.js";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, Brush, ReferenceLine } from 'recharts';
import Clock from "./Clock.js";
import { Select, MenuItem } from '@material-ui/core/';


class ChartContainer extends Component {

    constructor() {
        super();
        this.state = {
            currentDataKey: "ldsa"
        }

    }

    componentDidMount() {

    }

    handleSelectDataKeyChange(event) {
        this.setState({
            currentDataKey: event.target.value
        })
    }


    render() {
        let data = this.props.data;

        if (data.length > 0) {
            return (
                <div className="container">
                    <h3>Chart Container</h3>

                    <div className="App-intro">
                        <p>Amount of data in chart-array: {data.length}</p>
                        {/*
                        <Clock youngestEntry={this.dataStore[0].date} ></Clock>
                        <p>Youngest Entry: {this.dataStore[0].date.toLocaleString()}</p>
                        <p>Oldest Entry: {this.dataStore[this.dataStore.length - 1].date.toLocaleString()}</p>
                        */}
                    </div>

                    <Select
                        value={this.state.currentDataKey}
                        onChange={(event) => this.handleSelectDataKeyChange(event)}
                        inputProps={{
                            name: 'selectDataKey',
                            id: 'selectDataKey',
                        }}
                    >

                        <MenuItem value={"ldsa"}>LDSA</MenuItem>
                        <MenuItem value={"humidity"}>Humidity</MenuItem>
                        <MenuItem value={"diameter"}>Diameter</MenuItem>
                        <MenuItem value={"batteryVoltage"}>BatteryVoltage</MenuItem>
                        <MenuItem value={"temp"}>Temperature</MenuItem>
                    </Select>

                    <LineChart width={0.9 * this.props.width} height={300} data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }} syncId="main_sync">

                        <XAxis dataKey="time" />
                        <YAxis />
                        <CartesianGrid strokeDasharray="3 3" />
                        <Tooltip />
                        <Legend />
                        <Brush></Brush>
                        <ReferenceLine y={9800} label="Max" stroke="red" />
                        <Line type="monotone" dataKey={this.state.currentDataKey} stroke="#8884d8" activeDot={{ r: 8 }} connectNulls={true} />
                    </LineChart>

                </div>
            )
        } else {
            return (<p>Please select a day you want to review</p>);
        }
    }

}



export default ChartContainer;





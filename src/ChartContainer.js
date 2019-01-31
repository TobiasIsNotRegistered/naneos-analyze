import React, { Component } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, Brush, ReferenceLine } from 'recharts';
import { Select, MenuItem, FormControl, Button, Typography, FormControlLabel } from '@material-ui/core/';
import LoaderSmall from './LoaderSmall';
import './ChartContainer.css';
import { IconButton } from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';
import Switch from '@material-ui/core/Switch';


class ChartContainer extends Component {

    constructor() {
        super();
        this.state = {
            currentDevice: "",
            currentDataKey1: "ldsa",
            currentDataKey2: "humidity",
            availableDays: [],
            currentDayString: "",
            currentDayIndex: 0,
            statusText: "",
            dataToDisplay: [],
            chartIsLoading: false,
            isListeningToChanges: false,
            sampleData: [
                { name: 'Page A', uv: 4000, pv: 2400, amt: 2400 },
                { name: 'Page B', uv: 3000, pv: 1398, amt: 2210 },
                { name: 'Page C', uv: 2000, pv: 9800, amt: 2290 },
                { name: 'Page D', uv: 2780, pv: 3908, amt: 2000 },
                { name: 'Page E', uv: 1890, pv: 4800, amt: 2181 },
                { name: 'Page F', uv: 2390, pv: 3800, amt: 2500 },
                { name: 'Page G', uv: 3490, pv: 4300, amt: 2100 },
            ]
        }
        this.referencesRTDB = [];
        this.updateChart = this.updateChart.bind(this);
        this.registerListenersForThisDay = this.registerListenersForThisDay.bind(this);
    }

    componentDidMount() {
        if (this.props.devices.length > 0) {
            this.setState({ currentDevice: this.props.devices[0] });

        } else {
            this.setState({ currentDevice: "" });
        }
        this.loadDataForThisDevice(this.props.devices[0]);
    }

    componentWillUnmount() {
        //TODO: clean up local data
        this.cleanUpLocalData();
    }

    toggleListener() {
        if (this.state.isListeningToChanges) {
            this.cleanUpLocalData();
            this.setState({ isListeningToChanges: false })
        } else {
            this.registerListenersForThisDay();
            this.setState({ isListeningToChanges: true })
        }
    }

    cleanUpLocalData() {
        this.referencesRTDB.forEach(ref => {
            ref.off();
            console.log("Removed listener on: " + ref)
        })
        this.referencesRTDB = [];
    }

    registerListenersForThisDay() {
        const currentDevice = this.state.currentDevice;
        const currentDay = this.state.currentDayString;

        let dbKey = this.props.email.replace(/\./g, ",");
        //register listener on dataObjects - invokes when new data is added
        const dayRef = this.props.rtdb.ref(dbKey + "/" + currentDevice + "/" + currentDay);
        dayRef.limitToLast(1).on('child_added', (snap_dataObject) => {
            let date = new Date();
            const dataObject = snap_dataObject.val();
            dataObject.timeShort = new Date(snap_dataObject.val().date.time).toLocaleTimeString();
            /* only push new objects when available days is filled with data - this ensures that no duplicates are entered at the first load of the website */
            if (this.state.availableDays && this.state.availableDays.length > 0) {
                let dataPerDay = this.state.availableDays[this.state.currentDayIndex];
                if (dataObject.date.time != dataPerDay.data[dataPerDay.data.length - 1].date.time) {
                    dataPerDay.data.push(dataObject);
                    this.setState({ dataToDisplay: dataPerDay.data });
                    this.updateChart();
                    console.log(date.toLocaleTimeString() + ": found and inserted new data from: " + currentDevice + " with value: " + dataObject);
                }
            }
        })
        console.log("Registered listener for new data on: " + dbKey + "/" + currentDevice + "/" + currentDay);
        this.referencesRTDB.push(dayRef);
    }

    loadDataForThisDevice(serial) {
        let dbKey = this.props.email.replace(/\./g, ",");
        const dataRef = this.props.rtdb.ref(dbKey + "/" + serial);
        this.referencesRTDB.push(dataRef);
        console.log("Registered listener for new days on: " + dataRef);
        //clear existing data before reloading it to avoid duplicates 
        //firebase handles the cache and doesn't automatically download again - should be future-proof!
        this.setState({ availableDays: [], currentDayString: "", chartIsLoading: true, });

        //listener on days - invokes when a new day is added
        dataRef.limitToLast(1440).orderByKey().on('child_added', (snap_day) => {
            console.log('found day from rtdb: ' + snap_day.key + " for device: " + this.state.currentDevice)

            const dataPerDay = {
                key: snap_day.key,
                data: []
            }

            //this.registerListenersForThisDay(serial, snap_day); //uncomment this if you wanna listen to all devices all the time

            snap_day.forEach((_dataObject) => {
                const dataObject = _dataObject.val();
                if (_dataObject.val().date) {
                    dataObject.timeShort = new Date(_dataObject.val().date.time).toLocaleTimeString();
                } else {
                    dataObject.timeShort = "error"
                }


                dataPerDay.data.push(dataObject);
            })
            dataPerDay.data = dataPerDay.data.slice(0, 1440);

            this.setState(prevState => ({
                availableDays: [...prevState.availableDays, dataPerDay],
                currentDayString: snap_day.key,
                currentDayIndex: this.state.availableDays.length,
                dataToDisplay: dataPerDay.data,
                chartIsLoading: false
            }))
        })
    }

    displayDataForThisDay() {
        this.setState({
            dataToDisplay: this.state.availableDays[this.state.currentDayIndex].data
        })
    }

    //stupid function to ensure rerendering of the chart because ReChart doesn't compare length of old and new Array, it doesn't realize when new data is apparent except when it is served a different array, which is achieved with a simple splice function
    //splice removes elements from index a to b-1 in an array and returns the removed elements as a new array
    updateChart() {
        this.setState({
            dataToDisplay: this.state.dataToDisplay.slice()
        })
    }

    render() {
        if (this.props.devices.length > 0) {
            return (
                <div className="container">
                    <IconButton className="chart-container-btn-close" onClick={() => this.props.removeMyself()}><CloseIcon /></IconButton>
                    <FormControlLabel className="chart-container-btn-close"
                        control={
                            <Switch
                                checked={this.state.isListeningToChanges}
                                onChange={() => this.toggleListener()}
                                value="checkedB"
                                color="primary"
                            />
                        }
                        label="listen for new data" />

                    <Typography variant="h6" component="h3">
                         Chart N°{this.props.index+1} : {this.state.dataToDisplay.length < 1440 ? (this.state.dataToDisplay.length + " records") : (this.state.dataToDisplay.length + " records -- WARNING: maximum reached - there might be more data on RTDB.")}
                    </Typography>


                    <div className="chart-container-options">
                        <FormControl disabled={this.state.chartIsLoading} className="chart-container-options-child">
                            <Select
                                value={this.state.currentDevice}
                                onChange={(event) => { this.setState({ currentDevice: event.target.value }), this.loadDataForThisDevice(event.target.value) }}
                                inputProps={{
                                    name: 'selectDeviceKey',
                                    id: 'selectDeviceKey',
                                }}
                            >
                                {this.props.devices.map(device => {
                                    return (
                                        <MenuItem value={device}>{device}</MenuItem>
                                    )
                                })}
                            </Select>
                        </FormControl>

                        
                        <FormControl disabled={this.state.chartIsLoading} className="chart-container-options-child">
                            <Select
                                value={this.state.currentDayString}
                                onChange={(event, child) => {this.setState({currentDayString: event.target.value, currentDayIndex: child.props.id}) ,this.displayDataForThisDay() }}
                                inputProps={{
                                    name: 'selectDayKey',
                                    id: 'selectDayKey',
                                }}
                            >
                                {this.state.availableDays.map((day, i) => {
                                    return (
                                        <MenuItem id={i} value={day.key}>{day.key}</MenuItem>
                                    )
                                })}
                            </Select>
                        </FormControl>

                        <FormControl disabled={this.state.chartIsLoading} className="chart-container-options-child">
                            <Select
                                value={this.state.currentDataKey1}
                                onChange={(event) => this.setState({ currentDataKey1: event.target.value })}
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
                        </FormControl>

                    </div>

                    {!this.state.chartIsLoading ?
                        <LineChart width={this.props.width - 150} height={330} data={this.state.dataToDisplay} margin={{ top: 5, right: 30, left: 20, bottom: 5 }} syncId="main_sync">
                            <XAxis dataKey="timeShort" />
                            <YAxis />
                            <CartesianGrid strokeDasharray="3 3" />
                            <Tooltip formatter={(value) => new Intl.NumberFormat('en').format(value)} />
                            <Legend />
                            <Brush></Brush>
                            <ReferenceLine y={9800} label="Max" stroke="red" />
                            <Line type="monotone" dataKey={this.state.currentDataKey1} stroke="#8884d8" activeDot={{ r: 8 }} connectNulls={true} dot={false} />
                        </LineChart>

                        :

                        <LoaderSmall currentDevice={this.state.currentDevice} />
                    }
                </div>
            )
        } else {
            return (
                <div>
                    <h4>Error: No device found for {this.props.email}. Check List of devices on RTDB @ {this.props.email}/devices to ensure that data can be retrieved.</h4>
                    <h4><a href="https://console.firebase.google.com/u/2/project/analyze-naneos/database/analyze-naneos/data"> -->take me to firebase </a></h4>
                    <a href="mailto:martin.fierz@naneos.ch">--> send mail to admin</a>
                </div>
            )
        }
    }
}

export default ChartContainer;





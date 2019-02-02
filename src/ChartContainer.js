import React, { Component } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, Brush, ReferenceLine, Scatter, ScatterChart } from 'recharts';
import { Select, MenuItem, FormControl, Button, Typography, FormControlLabel, InputLabel } from '@material-ui/core/';
import LoaderSmall from './LoaderSmall';
import './ChartContainer.css';
import { IconButton } from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';
import Switch from '@material-ui/core/Switch';
import moment from 'moment'


class ChartContainer extends Component {

    constructor() {
        super();
        this.state = {
            currentDevice: "",
            currentDataKey1: "",
            currentDataKey2: "humidity",
            availableDays: [],
            currentDayString: "",
            currentDayIndex: 0,
            statusText: "",
            dataToDisplay: [],
            chartIsLoading: false,
            isListeningToChanges: false,
            listeningToChangesFrom: "",
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
        this.setState({currentDevice : ""});
        /*
        if (this.props.devices.length > 0) {
            this.setState({ currentDevice: this.props.devices[0] });

        } else {
            this.setState({ currentDevice: "" });
        }
        */
        //this.loadDataForThisDevice(this.props.devices[0]);
    }

    componentWillUnmount() {
        //TODO: clean up local data
        this.cleanUpLocalData();
    }

    toggleListener() {
        if (this.state.isListeningToChanges) {
            this.cleanUpLocalData();
            this.setState({ isListeningToChanges: false, listeningToChangesFrom: null })
        } else {
            this.registerListenersForThisDay();
            const x = {day: this.state.currentDay, device: this.state.currentDevice};
            this.setState({ isListeningToChanges: true, listeningToChangesFrom: x});
        }
    }

    cleanUpLocalData() {
        //callin reference.off() also omits the previously downloaded data, use with care
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
            const _dataObject = snap_dataObject.val();

            if (snap_dataObject.val().date) {
                _dataObject.timeShort = new Date(snap_dataObject.val().date.time).toLocaleTimeString();
                _dataObject.time = snap_dataObject.val().date.time;
            } else if(snap_dataObject.val().milliseconds){
               _dataObject.timeShort = new Date(snap_dataObject.val().milliseconds).toLocaleTimeString();
               _dataObject.milliseconds = snap_dataObject.val().milliseconds;
               _dataObject.time = snap_dataObject.val().milliseconds;
            }else{
                _dataObject.timeShort = "Error";
            }
           
            /* only push new objects when available days is filled with data - this ensures that no duplicates are entered at the first load of the website */
            if (this.state.availableDays && this.state.availableDays.length > 0) {
                let dataPerDay = this.state.availableDays[this.state.currentDayIndex];
                if (_dataObject.timeShort != dataPerDay.data[dataPerDay.data.length - 1].timeShort) {
                    dataPerDay.data.push(_dataObject);
                    this.setState({ dataToDisplay: dataPerDay.data });
                    this.updateChart();
                    console.log(date.toLocaleTimeString() + ": found and inserted new data from: " + currentDevice + " with value: " + _dataObject);
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
        dataRef.orderByKey().on('child_added', (snap_day) => {
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
                    dataObject.time = _dataObject.val().date.time;
                } else if(_dataObject.val().milliseconds){
                    dataObject.timeShort = new Date(_dataObject.val().milliseconds).toLocaleTimeString();
                    dataObject.milliseconds = _dataObject.val().milliseconds;
                    dataObject.time = _dataObject.val().milliseconds;
                    //let mydate = new Date(_dataObject.val().milliseconds);
                    //dataObject.time = mydate.getHours() + mydate.getMinutes()/60; 
                }else{
                    dataObject.timeShort = "Error";
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

            //this.sortDataAfterDays();
        })
    }

    displayDataForThisDay(index) {
        console.log("this.state.currentDayIndex: " + this.state.currentDayIndex + " / child.props.id: " + index);
        this.setState({
            dataToDisplay: this.state.availableDays[index].data
        })

    }

    //stupid function to ensure rerendering of the chart because ReChart doesn't compare length of old and new Array, it doesn't realize when new data is apparent except when it is served a different array, which is achieved with a simple splice function
    //splice removes elements from index a to b-1 in an array and returns the removed elements as a new array
    updateChart() {
        this.setState({
            dataToDisplay: this.state.dataToDisplay.slice()
        })
    }

    sortDataAfterDays(){
        //arg2 - arg1 = absteigende Reihenfolge = jüngstes Element zuerst (sort() benötigt einen Komparator der Zahlen <0; =0, oder >0 ausgibt)
        let _temp = this.state.availableDays;
        _temp.sort((day1, day2) => {return(day2.data[0].date.time - day1.data[0].date.time)})
        /*
        this.setState({
            availableDays : this.state.availableDays.sort((day1, day2) => {return(day2.data[0].date.time - day1.data[0].date.time)})
        })
        */
       return _temp;
    }

    render() {
        if (this.props.devices.length > 0) {
            return (
                <div className="container">
                    <IconButton className="chart-container-float-right-btn-close" onClick={() => this.props.removeMyself()}><CloseIcon /></IconButton>
                    <FormControl disabled={this.state.chartIsLoading || !(typeof this.state.dataToDisplay !== 'undefined' && this.state.dataToDisplay.length > 0)} className="chart-container-float-right-toggle-listening">
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={this.state.isListeningToChanges}
                                    onChange={() => this.toggleListener()}
                                    value="checkedB"
                                    color="primary"
                                />
                            }
                            label="listen for new data" />
                    </FormControl>


                    {/*<Typography variant="h6" component="h3">
                        Chart N°{this.props.index + 1} : {this.state.dataToDisplay.length < 1440 ? (this.state.dataToDisplay.length + " records") : (this.state.dataToDisplay.length + " records -- WARNING: maximum reached - there might be more data on RTDB.")}
                        </Typography>*/}


                    <div className="chart-container-options">
                        <FormControl disabled={this.state.chartIsLoading} className="chart-container-options-child">
                            <InputLabel >serial of device</InputLabel>
                            <Select
                                value={this.state.currentDevice}
                                onChange={(event) => { this.setState({ currentDevice: event.target.value, currentDataKey1: this.state.currentDataKey1 != "" ?  this.state.currentDataKey1 : "ldsa"}), this.loadDataForThisDevice(event.target.value)}}
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


                        <span>
                            <FormControl disabled={this.state.chartIsLoading || !(typeof this.state.dataToDisplay !== 'undefined' && this.state.dataToDisplay.length > 0)} className="chart-container-options-child">
                                <InputLabel >day of record</InputLabel>
                                <Select
                                    value={this.state.currentDayString}
                                    onChange={(event, child) => { this.setState({ currentDayString: event.target.value, currentDayIndex: child.props.id }), this.displayDataForThisDay(child.props.id) }}
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

                            <FormControl disabled={this.state.chartIsLoading || !(typeof this.state.dataToDisplay !== 'undefined' && this.state.dataToDisplay.length > 0)} className="chart-container-options-child">
                                <InputLabel >data</InputLabel>
                                <Select
                                    value={this.state.currentDataKey1}
                                    onChange={(event) => this.setState({ currentDataKey1: event.target.value })}
                                    inputProps={{
                                        name: 'selectDataKey',
                                        id: 'selectDataKey',
                                    }}
                                >
                                    <MenuItem value={"ldsa"}>LDSA</MenuItem>
                                    <MenuItem value={"numberC"}>Particle number</MenuItem>
                                    <MenuItem value={"diameter"}>Diameter</MenuItem>
                                    <MenuItem value={"humidity"}>Relative Humidity</MenuItem>
                                    <MenuItem value={"temp"}>Temperature</MenuItem>
                                    <MenuItem value={"batteryVoltage"}>Battery Voltage</MenuItem>
                                    <MenuItem value={"error"}>Device Status</MenuItem>
                                </Select>
                            </FormControl>
                        </span>
                    </div>

                    {!this.state.chartIsLoading ?
                        (this.state.dataToDisplay && this.state.dataToDisplay.length > 0 ?
                            (<div>
                                {/*
                                <LineChart width={this.props.width * 0.95} height={330} data={this.state.dataToDisplay} margin={{ top: 5, right: 30, left: 20, bottom: 5 }} syncId="main_sync" className="chart-container-graph">
                                <XAxis dataKey="timeShort" />
                                <YAxis width={0} />
                                <CartesianGrid strokeDasharray="3 3" />
                                <Tooltip formatter={(value) => new Intl.NumberFormat('en').format(value)} />
                                
                                <Brush height={20}></Brush>
                                <ReferenceLine y={9800} label="Max" stroke="red" />
                                <Line  isAnimationActive={false} 
                                    type="monotone" 
                                    dataKey={this.state.currentDataKey1} 
                                    stroke="#8884d8" 
                                    activeDot={{ r: 8 }} 
                                    connectNulls={true} 
                                    dot={false} />
                            </LineChart>
                                */}
                            
                            {
                                <ScatterChart width={this.props.width * 0.92} 
                                height={300} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                                    <CartesianGrid />
                                    <XAxis dataKey={'time'}  
                                        name='time'  
                                        type="number" 
                                        domain={['auto', 'auto']}
                                        tickFormatter = {(time) => moment(time).format('HH:mm')}
                                        />
                                    {/*<XAxis dataKey="timeShort" />*/}
                                    <YAxis width={0} 
                                        dataKey={this.state.currentDataKey1} 
                                        type="number" 
                                        name={this.state.currentDataKey1} />
                                    <Scatter name='Scatter plot' 
                                        data={this.state.dataToDisplay} 
                                        shape ='circle'
                                         fill='#8884d8' 
                                        isAnimationActive={false} 
                                        />
                                    <Tooltip cursor={{ strokeDasharray: '3 3' }}  
                                        formatter={(value) => new Intl.NumberFormat('en').format(value)}/>
                                    <Brush height={20}></Brush>
                                    
                                </ScatterChart>

                            }

                            </div>)
                            : <p>Please choose a device from the dropdown menu</p>)
                        :

                        <LoaderSmall currentDevice={this.state.currentDevice} />
                    }
                </div>
            )
        } else {
            return (
                <div>
                    <h4>Error: No device found for {this.props.email}. Check List of devices on RTDB @ {this.props.email}/devices to ensure that data can be retrieved.</h4>
                    <p>To create a list of devices, try syncing data with the Naneos-Analyze App for Android at least once.</p>
                    <a href="https://console.firebase.google.com/u/2/project/analyze-naneos/database/analyze-naneos/data"> -->take me to firebase </a>
                    <br/>   >
                    <a href="mailto:martin.fierz@naneos.ch">--> send mail to admin</a>
                </div>
            )
        }
    }
}

export default ChartContainer;





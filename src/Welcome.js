import React, { Component } from 'react';
import './Welcome.css';
import firebase from "./Firestore.js";
import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import TextField from '@material-ui/core/TextField';
import { CardContent, Snackbar, IconButton } from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';


class Welcome extends Component {

    constructor() {
        super();
        this.state = {
            email: "failed",
            password: "failed",
            snackBarOpened: false,
            snackBarMessage: "snackBar default message - failed to load error message"
        }
        this.handleCloseSnackbar = this.handleCloseSnackbar.bind(this);
    }

    componentDidMount() {

    }

    attemptLogin() {
        let self = this;
        this.props.setLoading(true);
        firebase.auth().signInWithEmailAndPassword(this.state.email, this.state.password).catch(function (error) {
            // Handle Errors here.
            /*
            var errorCode = error.code;
            var errorMessage = error.message;
            */
            // ...
            let msg = error.code + ": " + error.message;
            self.setState({
                snackBarMessage: msg,
                snackBarOpened: true
            });
        });
        console.log(this.state.email + " / " + this.state.password)
    }

    handleCloseSnackbar() {
        this.setState({ snackBarOpened: false });
    }

    render() {
        return (
            <div className="Welcome">
                <Card className="Welcome--CardLogin">
                    <CardContent>
                        <Typography color="textSecondary" gutterBottom>
                            Naneos - Analyze
                </Typography>
                        <Typography variant="h5" component="h2">
                            Welcome
                </Typography>


                        <TextField className="Welcome--TextField"
                            label="E-Mail"
                            onChange={(e) => this.setState({ email: e.target.value })}
                            margin="normal"
                        />

                        <TextField className="Welcome--TextField"
                            label="Password"
                            onChange={(x) => this.setState({ password: x.target.value })}
                            margin="normal"
                        />
                    </CardContent>


                    <CardActions className="Welcome--Button">
                        <Button onClick={() => this.attemptLogin()}>Login</Button>
                    </CardActions>
                </Card>

                <Snackbar
                    anchorOrigin={{
                        vertical: 'bottom',
                        horizontal: 'left',
                    }}
                    open={this.state.snackBarOpened}
                    autoHideDuration={6000}
                    onClose={() => this.handleCloseSnackbar()}
                    ContentProps={{
                        'aria-describedby': 'message-id',
                    }}
                    message={<span id="message-id">{this.state.snackBarMessage}</span>}
                    action={[
                        <IconButton
                            key="close"
                            aria-label="Close"
                            color="inherit"
                            onClick={() => this.handleCloseSnackbar()}
                        >
                            <CloseIcon />
                        </IconButton>,
                    ]}
                />
            </div>
        )
    }
}



export default Welcome;
import React from 'react';
import ReactDOM from 'react-dom';
import {Router, Route, hashHistory, Redirect} from 'react-router';

/* components */
import Layout from './components/Layout';
import UserLayout from './components/UserLayout';

/* scenes */
import Login from './scenes/Login';
import MyAccount from './scenes/MyAccount';

/* store */
import {Provider} from 'react-redux';
import Store from './store';

/* services */
import {RouteGuestOnly} from 'services/Login';

Store().then((store) => {

  ReactDOM.render(
    <Provider store={store}>
      <Router history={hashHistory}>
        <Route component={Layout}>
          <Route component={UserLayout} /*onEnter={RouteAuthorizedOnly(store)}*/>
            <Route path="/account" component={MyAccount}/>
          </Route>
          <Route onEnter={RouteGuestOnly(store)}>
            <Route path="/login" component={Login}/>
          </Route>
        </Route>
        <Redirect from="*" to="/login"/>
      </Router>
    </Provider>,
    document.getElementById('app')
  );

});

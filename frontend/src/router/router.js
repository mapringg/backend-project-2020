import Vue from 'vue';
import Router from 'vue-router';
import Login from '../views/Login';
import Logout from '../views/Logout';
import Main from '../views/Main.vue'

Vue.use(Router);

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/logout',
      name: 'Logout',
      component: Logout
    },
    {
      path: '/',
      name: 'Main',
      component: Main
    }
  ]
});

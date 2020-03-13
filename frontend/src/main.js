import Vue from 'vue'
import App from './App.vue'
import router from './router/router';
import vuetify from './plugins/vuetify';

Vue.config.productionTip = false

import './assets/custom.css';

new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount('#app')

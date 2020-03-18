<template>
  <v-form id="login-form" @keydown.enter="login">
    <h2 class="signin">Sign in with these providers</h2>
    <a href="/auth/google">
      <i class="fa-10x fab fa-facebook" style="color:blue"></i>
    </a>
    <!-- <a @click="onGoogle()">
      <i class="fa-10x fab fa-google-plus" style="color:red"></i>
    </a>-->
    <a href="http://localhost:3000/auth/google">
      <i class="fa-10x fab fa-google-plus" style="color:red"></i>
    </a>
    <a href="/auth/google" style="display:flex">
      <img src="../assets/Mahidol_Logo.png" class="MUIC_logo" />
    </a>
  </v-form>
</template>

<script>
// import loginService from "../services/login.service";
// import axios from "axios";
export default {
  mounted() {
    // to get the cookie data whenever component is mounted.
    // this.checkIfLoggedIn();
  },
  methods: {
    // checkIfLoggedIn() {
    //   axios.get("http://localhost:3000/auth/check").then(response => {
    //     console.log(response.data);
    //   });
    // }
    onGoogle() {
      // window.open("http://localhost:3000/auth/google")
      // axios.get("http://localhost:3000/auth/google").then(response => {
      //   console.log(response.data);
      // });
    }
  }
};
</script>

<style scoped>
#login-form {
  display: grid;
  grid-template-areas:
    "signin signin signin"
    "facebook google github";
  justify-items: center;
  grid-template-rows: 1fr 3fr;
  width: 50%;
  border-radius: 15px;
  background-color: rgb(235, 235, 235);
  padding: 25px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  align-items: center;
}
.signin {
  grid-area: signin;
  align-self: start;
}
.fa-facebook-square {
  grid-area: facebook;
}
.fa-google-plus-square {
  grid-area: google;
}
.MUIC_logo {
  width: 158px;
  height: 160px;
  grid-area: github;
}
</style>

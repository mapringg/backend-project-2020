<template>
  <div id="login">
    <v-container>
      <LoginForm />
    </v-container>
  </div>
</template>

<script>
import LoginForm from "../components/LoginForm";
export default {
  name: "Login",
  components: {
    LoginForm
  }
};
</script>

<style scoped>
#login {
  background-image: radial-gradient(
    circle farthest-corner at 10% 20%,
    rgba(0, 107, 141, 1) 0%,
    rgba(0, 69, 91, 1) 90%
  );
  background-size: 100% 100%;
  height: 100%;
}
</style>

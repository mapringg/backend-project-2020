<template>
  <v-form id="login-form" @keydown.enter="login">
    <h1 class="signin">Sign in with these providers</h1>
    <a href="/auth/google">
      <i class="fa-10x fab fa-facebook-square"></i>
    </a>
    <a href="/auth/google">
      <i class="fa-10x fab fa-google-plus-square" style="color:red"></i>
    </a>
    <a href="/auth/google">
      <!-- <i class="fa-10x fab fa-github-square" style="color:gray"></i> -->
    </a>
  </v-form>
</template>

<script>
export default {};
</script>

<style scoped>
#login-form {
  display: grid;
  grid-template-areas:
    "signin signin signin"
    "facebook google github";
  justify-items: center;
  grid-template-rows: 2fr 3fr;
  width: 50%;
  border-radius: 15px;
  background-color: white;
  padding: 25px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
}
.signin {
  grid-area: signin;
  align-self: center;
}
.fa-facebook-square {
  grid-area: facebook;
}
.fa-google-plus-square {
  grid-area: google;
}
.fa-github-square {
  grid-area: github;
}
</style>

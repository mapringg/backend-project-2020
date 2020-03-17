<template>
  <div></div>
</template>

<script>
import axios from "axios";
export default {
  name: "Main",
  mounted() {
    // to get the cookie data whenever component is mounted.
    this.checkIfLoggedIn();
  },
  methods: {
    checkIfLoggedIn() {
      axios
        .get("http://localhost:3000/profile", { withCredentials: true })
        .then(response => {
          console.log(response.data.user);
        });
    }
  }
};
</script>

<style lang="scss" scoped>
#card-file-input {
  height: 25px;
  width: 60%;
}
</style>
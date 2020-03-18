<template>
  <div></div>
</template>

<script>
import axios from "axios";
export default {
  name: "Logout",
  mounted() {
    this.logout();
  },
  methods: {
    logout() {
      axios
        .get("http://localhost:3000/auth/logout", { withCredentials: true })
        .then(() => {
          this.$router.push("/");
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

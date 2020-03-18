import axios from 'axios';

// TODO change localhost:3000 to nginx OAuth2 endpoint.
export default {
    async googleLogin() {
        const res = await axios.get('http://localhost:3000/auth/google');
        return res.data;
    },
};
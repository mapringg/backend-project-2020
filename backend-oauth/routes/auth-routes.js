const router = require('express').Router();
const passport = require('passport');

router.get('/logout', (req, res) => {
  req.logout();
  res.end();
});

router.get(
  '/google',
  passport.authenticate('google', {
    scope: ['profile'],
  })
);

router.get('/google/redirect', passport.authenticate('google'), (req, res) => {
  res.redirect('http://localhost:8080/');
});

module.exports = router;

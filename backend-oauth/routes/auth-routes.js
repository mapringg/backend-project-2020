const router = require('express').Router();
const passport = require('passport');
const k8s = require('@kubernetes/client-node');

const kc = new k8s.KubeConfig();
kc.loadFromDefault();

const k8sApi = kc.makeApiClient(k8s.CoreV1Api);

router.get('/login', (req, res) => {
  // console.log(req.user);
  res.render('login', { user: req.user });
});

router.get('/logout', (req, res) => {
  try {
    k8sApi
      .listNamespacedPod('default')
      .then()
      .catch();
    req.logout();
    res.redirect('/');
    // console.log(req.user);
  } catch (error) {
    // console.log(error);
  }
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

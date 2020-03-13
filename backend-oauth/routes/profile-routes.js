const router = require('express').Router();

router.get('/', (req, res) => {
  res.render('profile', { user: req.user });
});

module.exports = router;

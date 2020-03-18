const router = require('express').Router();

router.get('/', (req, res) => {
  res.send({ user: req.user });
});

module.exports = router;

function fn() {
  var port = karate.properties['server.port'] || '8080';
  var config = {
    baseUrl: 'http://localhost:' + port
  };
  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 10000);
  return config;
}

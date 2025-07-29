import { useEffect, useState } from 'react';

function App() {
  const [msg, setMsg] = useState('');

  useEffect(() => {
    fetch('http://localhost:8080/ping')
      .then(res => res.text())
      .then(setMsg);
  }, []);

  return <h1>{msg || 'Loading...'}</h1>;
}

export default App;

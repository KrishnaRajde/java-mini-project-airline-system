import { useEffect, useState } from 'react'

// Java payment server runs on the same laptop (port 8085)
const API = 'http://' + window.location.hostname + ':8085/api/payment'

function App() {
  // payment reference comes from the QR code link, eg. ?ref=PAY4K7M2X
  const ref = new URLSearchParams(window.location.search).get('ref')

  const [payment, setPayment] = useState(null)
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)

  // load payment details when the page opens
  useEffect(() => {
    loadDetails()
  }, [])

  function loadDetails() {
    if (!ref) {
      setError('No payment reference found. Please scan the QR code again.')
      return
    }
    fetch(API + '/details?ref=' + ref)
      .then((res) => res.json())
      .then((data) => {
        if (data.ok) setPayment(data)
        else setError(data.message)
      })
      .catch(() => setError('Cannot connect to the airline server. Make sure your phone and laptop are on the same Wi-Fi.'))
  }

  // tells the Java server: success, fail or retry
  function send(action) {
    setBusy(true)
    fetch(API + '/' + action + '?ref=' + ref, { method: 'POST' })
      .then((res) => res.json())
      .then((data) => {
        setNote(data.message)
        setBusy(false)
        loadDetails()
      })
      .catch(() => {
        setNote('Cannot connect to the airline server.')
        setBusy(false)
      })
  }

  return (
    <div className="page">
      <header className="header">
        <div className="logo">✈ SkyWay Airlines</div>
        <div className="sub">Flight Ticket Payment</div>
      </header>

      {error && <div className="card error">{error}</div>}

      {!error && !payment && <div className="card">Loading payment details...</div>}

      {payment && (
        <div className="card">
          <div className="ref">
            Booking Reference <b>{payment.ref}</b>
          </div>

          <div className="route">
            <div>
              <div className="city">{payment.from}</div>
              <div className="small">From</div>
            </div>
            <div className="plane">✈</div>
            <div>
              <div className="city">{payment.to}</div>
              <div className="small">To</div>
            </div>
          </div>

          <table className="details">
            <tbody>
              <tr><td>Flight</td><td>{payment.flightNo} ({payment.airline})</td></tr>
              <tr><td>Departure</td><td>{payment.departure}</td></tr>
              <tr><td>Passengers</td><td>{payment.passengers}</td></tr>
              <tr><td>Seats</td><td>{payment.seats}</td></tr>
              <tr><td>Booked by</td><td>{payment.customer}</td></tr>
            </tbody>
          </table>

          <div className="amount">
            <span>Total Amount (incl. 5% GST)</span>
            ₹ {payment.amount.toFixed(2)}
          </div>

          {payment.status === 'PENDING' && (
            <div className="buttons">
              <button className="done" disabled={busy} onClick={() => send('success')}>
                Payment Done
              </button>
              <button className="fail" disabled={busy} onClick={() => send('fail')}>
                Payment Failed
              </button>
            </div>
          )}

          {payment.status === 'SUCCESS' && (
            <div className="result success">
              <b>Payment Successful</b>
              Your booking is confirmed. PNR: <b>{payment.pnr}</b>
              <br />Your ticket is shown on the laptop and sent to your email.
            </div>
          )}

          {payment.status === 'FAILED' && (
            <div className="result failed">
              <b>Payment Failed</b>
              No seat was booked.
              <br />Click <b>Retry Payment</b> on the laptop and scan the new QR code.
            </div>
          )}

          {note && <div className="note">{note}</div>}
        </div>
      )}

      <footer>SkyWay Airline Reservation System · Student Project</footer>
    </div>
  )
}

export default App

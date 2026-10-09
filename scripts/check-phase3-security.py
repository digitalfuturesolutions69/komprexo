"""Compatibility entry point: Phase4A authorized Billing supersedes Free-only policy.
All release isolation/FileProvider safeguards run in the current stricter audit.
Original Phase3 checker remains available in accepted baseline Git history.
"""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('check-phase4-security.py')),run_name='__main__')
